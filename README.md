MySQL Rule
==========

This package provides a JUnit rule that starts and initializes a mysql instance. A small amount of setup on your
target host is required.


Getting started
---------------

1 `cd /tmp && git clone git@github.com:upside-services/mysql-rule.git && cd mysql-rule`

2 Download your preferred mysql tar archive from http://www.mysql.com/downloads/.

  * Tested with mysql version [mysql-5.7.16-osx10.11-x86_64.tar.gz](http://dev.mysql.com/get/Downloads/MySQL-5.7/mysql-5.7.16-osx10.11-x86_64.tar.gz). YMMV for other versions.

  * If you download a different binary distribution, be aware that some of the mysql packages have the actual binary nested in the downloaded archive. If so, unpack that to access the binary. Example: `tar -xf mysql-5.7.15-osx10.11-x86_64.tar` produces: `mysql-5.7.15-osx10.11-x86_64.tar.gz` and `mysql-test-5.7.15-osx10.11-x86_64.tar.gz`. We want: `mysql-5.7.15-osx10.11-x86_64.tar.gz`
  
3 Run `sudo ./bin/setup-host.sh /path/to/mysql-5.7.15-osx10.11-x86_64.tar.gz`

4 You should now have `/opt/mysql-rule/` with `binary` and `template` directories.


Example Test Code
-----------------

```
package com.upside.test.mysql;

import org.flywaydb.core.Flyway;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for DB migration.
 */
public class TestDBMigration {

    @Rule
    public MySQLRule rule = MySQLRule.rule(
        "my-test-db",
        "my-test-user",
        "my-test-user-password",
        false, //No debug
        11111, //Use port 11111, will fail if port not available.
        new LocalFile() //Loads the mysql binary install from /opt/mysql-rule/binary
    );
    
    //Create rule with default settings:
    //Creates a new DB named: service with user: test password: test and a dynamic port.
    @Rule
    public MySQLRule defaultConfig = MySQLRule.default(); 

    @Test
    public void testMysqlServerStarts() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(this.rule.getDbUrl(),
                             this.rule.getDbUser(),
                             this.rule.getDbPassword());
        flyway.migrate();
        
        flyway = new Flyway();
        flyway.setDataSource(this.defaultConfig.getDbUrl(),
                             this.defaultConfig.getDbUser(),
                             this.defaultConfig.getDbPassword());
        flyway.migrate();
 
    }
}
```


Tests
-----

1. Run Getting Started
2. ./gradlew test


OS Support
----------

1. Amazon Linux (likely all Linux variants, just not tested).
2. OS X


TODO
----

1. Deploy to OSS artifact repo
