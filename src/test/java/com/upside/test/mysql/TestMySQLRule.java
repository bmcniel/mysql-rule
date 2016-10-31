package com.upside.test.mysql;

import org.flywaydb.core.Flyway;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for DB rule.
 */
public class TestMySQLRule {

    @Rule
    public MySQLRule rule1 = MySQLRule.defaultRule();

    @Rule
    public MySQLRule rule2 = MySQLRule.defaultRule();

    @Rule
    public MySQLRule rule3 = MySQLRule.defaultRule();

    @Test
    public void testMysqlServerStarts() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(this.rule1.getDbUrl(), this.rule1.getDbUser(), this.rule1.getDbPassword());
        flyway.migrate();

        flyway = new Flyway();
        flyway.setDataSource(this.rule2.getDbUrl(), this.rule2.getDbUser(), this.rule2.getDbPassword());
        flyway.migrate();

        flyway = new Flyway();
        flyway.setDataSource(this.rule3.getDbUrl(), this.rule3.getDbUser(), this.rule3.getDbPassword());
        flyway.migrate();
    }
}
