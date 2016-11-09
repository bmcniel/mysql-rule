package com.upside.test.mysql.core;

/**
 * Created by bsiemon on 11/9/16.
 */
public interface MySQLProcess {
    /**
     * Sends commands to the running mysqld instance.
     *
     * Calling this method before startAndWait() has retunred has undefined results.
     *
     * Useful for creating schemas, users and grants on a new mysqld instance.
     *
     * Example:
     *
     * <code>
     * sendClientCommands(
     *               String.format("CREATE DATABASE %s;", this.dbName),
     *               String.format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s';", this.dbUser, this.dbPassword),
     *               String.format("GRANT ALL ON %s.* TO '%s'@'localhost';", this.dbName, this.dbUser)
     * );
     * </code>
     * @param commands A list of mysql commands to execute.
     */
    void sendClientCommands(String... commands);

    /**
     * Starts the mysqld process and waits for mysqld to transition to fully online.
     *
     * @return A process instance for the created mysqld process.
     */
    Process startAndWait();

    /**
     * Stops the running mysqld process and performs any required cleanup.
     *
     * @return The stopped process instance.
     */
    Process stopAndCleanup();
}
