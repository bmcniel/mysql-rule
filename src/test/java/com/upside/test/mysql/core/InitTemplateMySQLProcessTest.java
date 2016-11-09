package com.upside.test.mysql.core;

import static org.junit.Assert.*;

/**
 * Created by bsiemon on 11/9/16.
 */
public class InitTemplateMySQLProcessTest implements MySQLProcess {
    private final MySQLProcess delegate;

    public InitTemplateMySQLProcessTest(MySQLProcess delegate) {
        this.delegate = delegate;
    }


    @Override
    public void sendClientCommands(String... commands) {

    }

    @Override
    public Process start() {
        return null;
    }

    @Override
    public Process startAndWait() {
        return null;
    }

    @Override
    public Process stopAndCleanup() {
        return null;
    }
}