MySQL Rule
==========

This package provides a JUnit rule that starts and initialized a mysql instance. A small amount of setup on your
target host is required by running `sudo bin/setup-host.sh /your/mysql/binary.tar.gz`.


Getting started
===============

1 Download your preferred mysql tar archive from http://www.mysql.com/downloads/.

  - Some of the mysql packages have the actual binary nested in the downloaded archive. If so unpack that to access the binary. Example: `tar -xf mysql-5.7.15-osx10.11-x86_64.tar` produces: `mysql-5.7.15-osx10.11-x86_64.tar.gz` and `mysql-test-5.7.15-osx10.11-x86_64.tar.gz`. We want: `mysql-5.7.15-osx10.11-x86_64.tar.gz`
  
2 Run `sudo ./bin/setup-host.sh /path/to/mysql-5.7.15-osx10.11-x86_64.tar.gz`

3 You should now have `/opt/mysql-rule/` with `binary` and `template` directories.


Example Test Code
=================

```
package com.upside.test.mysql;

import org.flywaydb.core.Flyway;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for DB migration.
 */
public class TestDBMigration {

    //Creates a new DB named: service with user: test password: test
    @Rule
    public MySQLRule rule = MySQLRule.default();

    @Test
    public void testMysqlServerStarts() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(this.rule.getDbUrl(),
                             this.rule.getDbUser(),
                             this.rule.getDbPassword());
        flyway.migrate();
    }
}
```
