package com.upside.test.mysql;

import com.upside.test.mysql.binary.LocalFile;
import com.upside.test.mysql.core.InitTemplateMySQLProcess;
import com.upside.test.mysql.core.LocalhostMysqlProcess;
import com.upside.test.mysql.core.MySQLProcess;
import com.upside.test.mysql.util.FileUtil;
import com.upside.test.mysql.util.MySQLUtil;
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

    private MySQLProcess mysqldProcess;
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

        this.mysqlRootDirectory = Files.createTempDirectory(
                buildDataPrefix(),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwx---")));
        Path templatePath = new File(binaryRoot, "template").toPath();
        try {
            this.mysqldProcess = new InitTemplateMySQLProcess(
                    new LocalhostMysqlProcess(
                            this.mysqlRootDirectory,
                            binaryRoot,
                            this.port,
                            this.debug),
                    this.mysqlRootDirectory,
                    templatePath);

            this.mysqldProcess.startAndWait();

            this.mysqldProcess.sendClientCommands(
                    String.format("CREATE DATABASE %s;", this.dbName),
                    String.format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s';", this.dbUser, this.dbPassword),
                    String.format("GRANT ALL ON %s.* TO '%s'@'localhost';", this.dbName, this.dbUser)
            );
        }
        catch (Exception e) {
            after();
            throw e;
        }
    }

    protected void after() {
        this.mysqldProcess.stopAndCleanup();
    }

    private String buildDataPrefix() {
        return "mysql-test-rule";
    }
}
