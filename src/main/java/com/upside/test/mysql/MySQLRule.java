package com.upside.test.mysql;

import com.upside.test.mysql.binary.LocalFile;
import com.upside.test.mysql.util.FileUtil;
import com.upside.test.mysql.util.SocketUtil;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;

/**
 * Rule that starts a mysql instance.
 */
public class MySQLRule extends ExternalResource {

    private MysqlBinaryLoader loader = new LocalFile();

    private Process mysqldProcess;
    private Path mysqlRootDirectory;
    private final int port;
    private final boolean debug;

    private final String dbName;
    private final String dbUser;
    private final String dbPassword;

    /**
     * @return An instance of MySQLRule with default values and debug enabled.
     */
    public static MySQLRule debug() {
        return new MySQLRule("service", "test", "test", true, null);
    }

    /**
     * Creates a default instance of the rule.
     * DB Name: service
     * DB User: test
     * DB Password: test
     * @return An initialized MysqlRule instance.
     */
    public static MySQLRule defaultRule() {
        return new MySQLRule("service", "test", "test", false, null);
    }

    /**
     * Creates an instance of the rule with the provided parameters.
     * @param dbName The database name to initialize
     * @param dbUser The user to create during setup
     * @param dbPassword The password to assign to the given user.
     * @param debug If True pipes the mysql startup to the hosting JVM stderr and stdout.
     * @param port The port to use for mysqld. If null a free port will be found at mysql start time.
     * @return An initialized MysqlRule instance.
     */
    public static MySQLRule rule(String dbName, String dbUser, String dbPassword, boolean debug, int port) {
        return new MySQLRule(dbName, dbUser, dbPassword, debug, port);
    }
    /**
     * Creates an instance of the rule with the provided parameters, set debug to false.
     * @param dbName The database name to initialize
     * @param dbUser The user to create during setup
     * @param dbPassword The password to assign to the given user.
     * @return An initialized MysqlRule instance.
     */
    public static MySQLRule rule(String dbName, String dbUser, String dbPassword) {
        return new MySQLRule(dbName, dbUser, dbPassword, false, null);
    }

    private MySQLRule(String dbName, String dbUser, String dbPassword, boolean debug, Integer port) {
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.debug = debug;
        if (port == null) {
            this.port = SocketUtil.findFreePort();
        }
        else {
            this.port = port;
        }
    }

    /**
     * @return The port to use for mysqld. Only valid after {@code after()} has run.
     */
    public int getPort() {
        return this.port;
    }

    /**
     * @return The configured DB Name.
     */
    public String getDbName() {
        return this.dbName;
    }

    /**
     * @return The configured DB user.
     */
    public String getDbUser() {
        return this.dbUser;
    }

    /**
     * @return The configured DB password.
     */
    public String getDbPassword() {
        return this.dbPassword;
    }

    /**
     * @return A jdbc connection string using the configured port and db name.
     *
     * Example:
     *
     * jdbc:mysql://localhost:11111/service
     */
    public String getDbUrl() {
        return String.format("jdbc:mysql://localhost:%s/%s", this.port, this.dbName);
    }

    protected void before() throws Throwable {

        File binaryRoot= loader.load()
                .orElseThrow(() -> new RuntimeException("Unable to load mysql binary."));

        //setup the root directories for this mysql instance

        this.mysqlRootDirectory = Files.createTempDirectory(
                buildDataPrefix(),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwx---")));

        String binaryPath = new File(binaryRoot,"binary/bin/mysqld").getAbsolutePath();
        String clientBinaryPath = new File(binaryRoot,"binary/bin/mysql").getAbsolutePath();


        String basePath = this.mysqlRootDirectory.toFile().getAbsolutePath();
        String dataPath = new File(this.mysqlRootDirectory.toFile(), "data").getAbsolutePath();
        String socketFile = new File(this.mysqlRootDirectory.toFile(), "socket").getAbsolutePath();

        Path templatePath = new File(binaryRoot, "template").toPath();

        if (templatePath.toFile().exists()) {
            FileUtil.copyDirectory(templatePath, this.mysqlRootDirectory);
        }
        else {
            throw new RuntimeException(
                    String.format("Unable to find template directory: %s", templatePath.toAbsolutePath()));
        }

        try {
            this.mysqldProcess = enableDebug(new ProcessBuilder(
                    binaryPath,
                    String.format("--basedir=%s", basePath),
                    String.format("--port=%s", this.port),
                    String.format("--socket=%s", socketFile),
                    String.format("--datadir=%s", dataPath))
                    .directory(this.mysqlRootDirectory.toFile()))
                    .start();

            if (!SocketUtil.waitForLocalSocketSocket(this.port)) {
               throw new RuntimeException("Server failed to start in time.");
            }

            final Process clientProcess =
                    new ProcessBuilder(
                            clientBinaryPath,
                            String.format("--port=%s", this.port),
                            "--protocol=TCP",
                            "--user=root",
                            "--password=")
                            .directory(this.mysqlRootDirectory.toFile())
                            .start();

            final PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(
                            clientProcess.getOutputStream()));

            writer.println(String.format("CREATE DATABASE %s;", this.dbName));
            writer.println(String.format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s';", this.dbUser, this.dbPassword));
            writer.println(String.format("GRANT ALL ON %s.* TO '%s'@'localhost';", this.dbName, this.dbUser));
            //Closing this writer closes the mysql client by ending the input stream.
            writer.close();
            clientProcess.waitFor(5, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            after();
            throw e;
        }
    }

    /**
     */
    protected void after() {
        this.mysqldProcess.destroy();
        try {
            this.mysqldProcess.waitFor(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            //as long as it died
        }
        FileUtil.deleteDirectory(this.mysqlRootDirectory);
    }

    private ProcessBuilder enableDebug(ProcessBuilder processBuilder) {
         if (this.debug) {
            processBuilder = processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        return processBuilder;
    }

    private String buildDataPrefix() {
        return "mysql-test-rule";
    }
}
