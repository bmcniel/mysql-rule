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
     *
     * @return An available free port.
     */
    public static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SocketUtil() {
    }
}
