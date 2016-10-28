package com.upside.test.mysql;

import java.io.File;
import java.util.Optional;

/**
 * Load a or use an existing mysql binary from a potentially missing source.
 */
public interface MysqlBinaryLoader {
    Optional<File> load();
}
