package com.upside.test.mysql;

import com.upside.test.mysql.binary.LocalFile;
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
    private int port = -1;

    private final String dbName;
    private final String dbUser;
    private final String dbPassword;

    public MySQLRule() {
        this("service", "test", "test");
    }

    public MySQLRule(String dbName, String dbUser, String dbPassword) {
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public int getPort() {
        return this.port;
    }

    public String getDbName() {
        return this.dbName;
    }

    public String getDbUser() {
        return this.dbUser;
    }

    public String getDbPassword() {
        return this.dbPassword;
    }

    public String getDbUrl() {
        return String.format("jdbc:mysql://localhost:%s/%s", this.port, this.dbName);
    }


    /**
     */
    protected void before() throws Throwable {

        File binaryRoot= loader.load()
                .orElseThrow(() -> new RuntimeException("Unable to load mysql binary."));

        //setup the root directories for this mysql instance

        this.mysqlRootDirectory = Files.createTempDirectory(
                buildDataPrefix(),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwx---")));
        System.out.println("MySQLRule Root: " + this.mysqlRootDirectory);

        this.port = SocketUtil.findFreePort();
        System.out.println("MySQLRule Port: " + this.port);

        String binaryPath = new File(binaryRoot,"bin/mysqld").getAbsolutePath();
        String clientBinaryPath = new File(binaryRoot,"bin/mysql").getAbsolutePath();
        String basePath = this.mysqlRootDirectory.toFile().getAbsolutePath();
        String dataPath = new File(this.mysqlRootDirectory.toFile(), "data").getAbsolutePath();
        String socketFile = new File(this.mysqlRootDirectory.toFile(), "socket").getAbsolutePath();

        Process initProcess = new ProcessBuilder(
                binaryPath,
               "--initialize-insecure",
                String.format("--basedir=%s", basePath),
                String.format("--port=%s", this.port),
                String.format("--socket=%s", socketFile),
                String.format("--datadir=%s", dataPath))
                .directory(this.mysqlRootDirectory.toFile())
//                .redirectError(ProcessBuilder.Redirect.INHERIT)
//                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start();

        if (!initProcess.waitFor(30, TimeUnit.SECONDS)) {
            initProcess.destroy();
            throw new RuntimeException(
                    String.format("Unable to initialize mysql db. Check logs: %s", this.mysqlRootDirectory));
        }

        int exitCode = initProcess.exitValue();
        if (exitCode != 0) {
           throw new RuntimeException("Unable to initialize mysql db.");
        }

        try {
            this.mysqldProcess = new ProcessBuilder(
                    binaryPath,
                    String.format("--basedir=%s", basePath),
                    String.format("--port=%s", this.port),
                    String.format("--socket=%s", socketFile),
                    String.format("--datadir=%s", dataPath))
                    .directory(this.mysqlRootDirectory.toFile())
//                    .redirectError(ProcessBuilder.Redirect.INHERIT)
//                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            if (!SocketUtil.waitForLocalSocketSocket(this.port)) {
               throw new RuntimeException("Server failed to start in time.");
            }

            final Process clientProcess =
                    new ProcessBuilder(
                            clientBinaryPath,
                            String.format("--socket=%s", socketFile),
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
        this.mysqlRootDirectory.toFile().delete();
    }

    private String buildDataPrefix() {
        return "mysql-test-rule";
    }
}
