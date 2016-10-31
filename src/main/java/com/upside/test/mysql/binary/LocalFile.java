package com.upside.test.mysql.binary;

import com.upside.test.mysql.MysqlBinaryLoader;

import java.io.File;
import java.util.Optional;

/**
 * Attempts to load the mysql binary for the ambient os and arch
 * from the local file system.
 */
public class LocalFile implements MysqlBinaryLoader {

    public static final String BINARY_SOURCE = "/opt/mysql-rule/";

    @Override
    public Optional<File> load() {
        File binarySource = new File(BINARY_SOURCE);
        if (binarySource.exists()) {
            return Optional.of(new File(BINARY_SOURCE));
        }
        else {
            return Optional.empty();
        }
    }
}
