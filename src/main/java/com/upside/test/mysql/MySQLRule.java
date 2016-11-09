package com.upside.test.mysql;

import com.upside.test.mysql.binary.LocalFile;
import com.upside.test.mysql.core.InitViaTemplateMySQLProcess;
import com.upside.test.mysql.core.LocalhostMySQLProcess;
import com.upside.test.mysql.core.MySQLProcess;
import com.upside.test.mysql.util.SocketUtil;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Rule that starts a mysql instance.
 */
public class MySQLRule extends ExternalResource {

    private final MysqlBinaryLoader loader;

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
        return new MySQLRule("service", "test", "test", true, null, new LocalFile());
    }

    /**
     * Creates a default instance of the rule.
     * DB Name: service
     * DB User: test
     * DB Password: test
     *
     * Uses the default mysql binary loader which expects mysql to be installed at /opt/mysql-rule/binary/
     *
     * @return An initialized MysqlRule instance.
     */
    public static MySQLRule defaultRule() {
        return new MySQLRule("service", "test", "test", false, null, new LocalFile());
    }

    /**
     * Creates an instance of the rule with the provided parameters.
     *
     * @param dbName The database name to initialize
     * @param dbUser The user to create during setup
     * @param dbPassword The password to assign to the given user.
     * @param debug If True pipes the mysql startup to the hosting JVM stderr and stdout.
     * @param port The port to use for mysqld. If null a free port will be found at mysql start time.
     * @param loader An implementation of {@link MysqlBinaryLoader} that returns the location of a mysql install.
     * @return An initialized MysqlRule instance.
     */
    public static MySQLRule rule(String dbName, String dbUser,
                                 String dbPassword, boolean debug, int port,
                                 MysqlBinaryLoader loader) {
        return new MySQLRule(dbName, dbUser, dbPassword, debug, port, loader);
    }
    /**
     * Creates an instance of the rule with the provided parameters, set debug to false.
     *
     * @param dbName The database name to initialize
     * @param dbUser The user to create during setup
     * @param dbPassword The password to assign to the given user.
     * @param loader An implementation of {@link MysqlBinaryLoader} that returns the location of a mysql install.
     * @return An initialized MysqlRule instance.
     */
    public static MySQLRule rule(String dbName, String dbUser, String dbPassword, MysqlBinaryLoader loader) {
        return new MySQLRule(dbName, dbUser, dbPassword, false, null, loader);
    }

    private MySQLRule(String dbName, String dbUser, String dbPassword,
                      boolean debug, Integer port, MysqlBinaryLoader loader) {
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
        this.loader = loader;
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
        return String.format("jdbc:mysql://localhost:%s/%s?autoReconnect=true&useSSL=false", this.port, this.dbName);
    }

    protected void before() throws Throwable {
        File binaryRoot= loader.load()
                .orElseThrow(() -> new RuntimeException("Unable to load mysql binary."));

        this.mysqlRootDirectory = Files.createTempDirectory(
                buildDataPrefix(),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwx---")));

        Path templatePath = new File("/opt/mysql-rule/template").toPath();

        try {
            this.mysqldProcess = new InitViaTemplateMySQLProcess(
                    new LocalhostMySQLProcess(
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
