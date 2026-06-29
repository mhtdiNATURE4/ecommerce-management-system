package com.market.ecommerce.reports;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShedLockMigrationIT {

    @Test
    public void flywayCreatesShedlockTable() throws Exception {
        String url = "jdbc:h2:mem:flyway_test;DB_CLOSE_DELAY=-1";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
                Flyway flyway = Flyway.configure()
                    .dataSource(url, "sa", "")
                    .locations("classpath:db/migration_test")
                    .load();
            flyway.migrate();

            try (Statement s = c.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM shedlock");
                boolean found = false;
                if (rs.next()) {
                    found = true;
                }
                assertTrue(found, "shedlock table should exist after migration");
            }
        }
    }
}
