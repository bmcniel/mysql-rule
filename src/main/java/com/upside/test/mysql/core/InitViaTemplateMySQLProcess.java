package com.upside.test.mysql.core;

import com.google.auto.value.processor.escapevelocity.Template;
import com.upside.test.mysql.util.FileUtil;

import java.io.File;
import java.nio.file.Path;

/**
 * Copies template direcotry to give mysql root directory. Effectively accomplishing mysql --init.
 *
 * Delegates the bits of starting an actual mysqld to another implementation.
 *
 * Requries that an initialized mysqld data directory be located at: /opt/mysql-rule/template.
 * How to do this is covered in the attached setup script.
 *
 * Probably don't want to do this for a production mysql :).
 */
public class InitViaTemplateMySQLProcess implements MySQLProcess {
    private final MySQLProcess delegate;
    private final Path mysqlRootDirectory;
    private final Path templateRoot;

    public InitViaTemplateMySQLProcess(MySQLProcess delegate, Path mysqlRootDirectory, Path templateRoot) {
        this.delegate = delegate;
        this.mysqlRootDirectory = mysqlRootDirectory;
        this.templateRoot = templateRoot;
    }

    @Override
    public void sendClientCommands(String... commands) {
        this.delegate.sendClientCommands(commands);
    }

    @Override
    public Process startAndWait() {
        this.copyTemplate();
        return this.delegate.startAndWait();
    }

    @Override
    public Process stopAndCleanup() {
        return this.delegate.stopAndCleanup();
    }

    private void copyTemplate() {
        if (this.templateRoot.toFile().exists()) {
            FileUtil.copyDirectory(this.templateRoot, this.mysqlRootDirectory);
        }
        else {
            throw new RuntimeException(
                    String.format("Unable to find template directory: %s", this.templateRoot.toAbsolutePath()));
        }
    }
}
