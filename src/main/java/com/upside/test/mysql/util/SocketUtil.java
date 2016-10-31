package com.upside.test.mysql.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Utility class to find a port to bind to for tests.
 */
public final class SocketUtil {
    /**
     * Returns a free port number on localhost, or throws a runtime exception on error.
     * @return An available free port.
     */
    public static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param port The port to wait for.
     * @return True if bound false if timeout waiting.
     */
    public static boolean waitForLocalSocketSocket(int port) {
        return waitForLocalSocketSocket(port, 15);
    }

    /**
     * @param port The port to wait for.
     * @param attempts The number of times to try and connect with a hardcoded wait interval.
     * @return True if bound false if timeout waiting.
     */
    public static boolean waitForLocalSocketSocket(int port, int attempts) {
        int count = 0;
        try {
            Thread.sleep(100);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        while (count < attempts) {
            try (Socket ignored = new Socket("localhost", port)) {
                return true;
            }
            catch (ConnectException e) {
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            count++;
        }
        return false;
    }

    private SocketUtil(){}
}
