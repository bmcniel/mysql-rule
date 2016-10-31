package com.upside.test.mysql;

import org.flywaydb.core.Flyway;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for DB rule.
 */
public class TestSingleMySQLRule {

    @Rule
    public MySQLRule rule = MySQLRule.debug();

    @Test
    public void testMysqlServerStarts() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(this.rule.getDbUrl(), this.rule.getDbUser(), this.rule.getDbPassword());
        flyway.migrate();
    }
}
