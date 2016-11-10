package com.upside.test.mysql.util;

import com.upside.test.mysql.MySQLRule;

import javax.security.auth.login.AccountException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by bsiemon on 11/9/16.
 */
public final class MySQLUtil {
    private MySQLUtil() {}

    /**
     * Returns true if the mysql process is alive. Uses localhost has the target host.
     *
     * @param adminPath The path to the admin binary.
     * @param port The port to attempt to connect to.
     * @return True if mysql is alive, false if not.
     */
    public static boolean pingMySQLProcess(String adminPath, int port) {
        try {
            Process pingProcess = new ProcessBuilder(
                     adminPath,
                     "ping",
                     "--protocol=TCP",
                     "--host=localhost",
                     "--silent",
                     "--user=root",
                     "--password=",
                     String.format("--port=%s", port))
                    .start();

            if (!pingProcess.waitFor(1, TimeUnit.SECONDS)) {
                pingProcess.destroy();
                return false;
            }

            return pingProcess.exitValue() == 0;
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean waitForMySQLToStart(String adminPath, int port) {
        return waitForMySQLToStart(adminPath, port, 200);
    }

    public static boolean waitForMySQLToStart(String adminPath, int port, int attempts) {
        wait100MS();
        int count = 0;
        while (count < attempts) {
            if (pingMySQLProcess(adminPath, port)) {
                return true;
            }
            wait100MS();
            count++;
        }
        return false;

    }

    private static void wait100MS() {
        try {
            Thread.sleep(100);
        }
        catch (InterruptedException e1) {
            throw new RuntimeException(e1);
        }
    }
}
