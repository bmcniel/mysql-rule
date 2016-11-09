package com.upside.test.mysql.core;

/**
 * Created by bsiemon on 11/9/16.
 */
public interface MySQLProcess {
    void sendClientCommands(String... commands);

    Process start();

    Process startAndWait();

    Process stopAndCleanup();
}
