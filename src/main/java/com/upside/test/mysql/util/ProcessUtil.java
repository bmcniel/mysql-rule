package com.upside.test.mysql.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by bsiemon on 11/9/16.
 */
public final class ProcessUtil {
    private ProcessUtil() {}

    public static void waitFor(Process process) {
        try {
            process.waitFor(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Process startBuilder(ProcessBuilder builder) {
        try {
            return builder.start();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
