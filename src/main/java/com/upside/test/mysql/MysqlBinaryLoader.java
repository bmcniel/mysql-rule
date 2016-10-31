package com.upside.test.mysql;

import java.io.File;
import java.util.Optional;

/**
 * Load a or use an existing mysql binary from a potentially missing source.
 */
public interface MysqlBinaryLoader {
    /**
     * Attempts to load the binary location of the mysql install.
     *
     * @return The File location or empty optional if unable to load.
     */
    Optional<File> load();
}
