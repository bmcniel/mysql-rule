package com.upside.test.mysql.core;

import com.upside.test.mysql.util.FileUtil;
import com.upside.test.mysql.util.MySQLUtil;
import com.upside.test.mysql.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Abstracts the mechanics of starting a mysql daemon.
 *
 * Requries the base directory to have been initialized via mysqld --initialize ...
 *
 * Notes about how mysqld is started:
 *
 * 1. Binds only to localhost.
 * 2. Uses default timezone of UTC.
 */
public class LocalhostMySQLProcess implements MySQLProcess {
    private final Path rootDirectory;
    private final String adminPath;
    private final String clientBinaryPath;
    private final String binaryPath;
    private final int port;

    private final ProcessBuilder processBuilder;
    private Process process;

    public LocalhostMySQLProcess(Path mysqlRoot, File binaryRoot, int port, boolean debug) {
        this.rootDirectory = mysqlRoot;

        this.binaryPath = new File(binaryRoot, "bin/mysqld").getAbsolutePath();
        this.clientBinaryPath = new File(binaryRoot, "bin/mysql").getAbsolutePath();
        this.adminPath = new File(binaryRoot, "bin/mysqladmin").getAbsolutePath();

        this.port = port;

        String basePath = mysqlRoot.toFile().getAbsolutePath();
        String dataPath = new File(mysqlRoot.toFile(), "data").getAbsolutePath();
        String socketFile = new File(mysqlRoot.toFile(), "socket").getAbsolutePath();

        this.processBuilder = enableDebug(new ProcessBuilder(
                binaryPath,
                "--bind-address=localhost",
                String.format("--basedir=%s", basePath),
                String.format("--port=%s", port),
                "--default-time-zone=+00:00",
                String.format("--socket=%s", socketFile),
                String.format("--datadir=%s", dataPath))
                .directory(mysqlRoot.toFile()), debug);
    }

    @Override
    public void sendClientCommands(String... commands) {
        final Process clientProcess;
        clientProcess = ProcessUtil.startBuilder(new ProcessBuilder(
                this.clientBinaryPath,
                String.format("--port=%s", this.port),
                "--protocol=TCP",
                "--user=root",
                "--password=")
                .directory(this.rootDirectory.toFile()));

        final PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        clientProcess.getOutputStream()));
        for (String c : commands) {
            writer.println(c);
        }
        writer.close();
        ProcessUtil.waitFor(clientProcess);
    }

   /**
     * Attempts to start the mysql process then wait for it to respond to mysqladmin ping for ~20 seconds.
     *
     * @return The process instance created.
     */
    @Override
    public Process startAndWait() {
        Process process = start();
        if (!MySQLUtil.waitForMySQLToStart(this.adminPath, this.port)) {
            throw new RuntimeException("Server failed to start in time.");
        }
        return process;
    }

    /**
     * Destroys the monitored mysqld process and deletes the provided root directory used by the procedss.
     *
     * @return The destroyed process instance.
     */
    @Override
    public Process stopAndCleanup() {
        if (this.process == null) {
            throw new IllegalStateException("Unable to stop process. Not started.");
        }

        this.process.destroy();

        ProcessUtil.waitFor(this.process);

        FileUtil.deleteDirectory(this.rootDirectory);

        return this.process;
    }

    private ProcessBuilder enableDebug(ProcessBuilder processBuilder, boolean debug) {
        if (debug) {
            processBuilder = processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        return processBuilder;
    }

    private Process start() {
        if (this.process != null) {
            throw new IllegalStateException("Unable to start process. Already started.");
        }
        this.process = ProcessUtil.startBuilder(this.processBuilder);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                process.destroyForcibly();
            }
        });

        return this.process;
    }
}
