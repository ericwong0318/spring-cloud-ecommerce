package com.example.order;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Applies the order-service Flyway migration scripts to a Testcontainers PostgreSQL instance.
 *
 * <p>Scripts are executed directly over JDBC rather than through Flyway, because Flyway's
 * version detection is unreliable against the PostgreSQL versions used by the tests.
 */
public final class TestDatabaseMigrations {

    private static final String[] MIGRATION_SCRIPTS = {
        "db/migration/V1__create_order_table.sql",
        "db/migration/V2__create_outbox_table.sql",
        "db/migration/V3__create_processed_events_table.sql",
        "db/migration/V4__create_order_item_table.sql",
        "db/migration/V4__create_shedlock_table.sql",
        "db/migration/V5__create_shipment_tables.sql"
    };

    private TestDatabaseMigrations() {
    }

    public static void run(PostgreSQLContainer<?> postgres) {
        try {
            org.apache.commons.dbcp2.BasicDataSource dataSource = new org.apache.commons.dbcp2.BasicDataSource();
            dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s",
                    postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
            dataSource.setUsername(postgres.getUsername());
            dataSource.setPassword(postgres.getPassword());
            dataSource.setDriverClassName("org.postgresql.Driver");

            try (java.sql.Connection conn = dataSource.getConnection()) {
                for (String script : MIGRATION_SCRIPTS) {
                    System.out.println("Running migration: " + script);
                    try (java.io.InputStream is = TestDatabaseMigrations.class.getClassLoader().getResourceAsStream(script)) {
                        if (is == null) {
                            throw new RuntimeException("Migration script not found: " + script);
                        }
                        String sql = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        for (String statement : sql.split(";")) {
                            String trimmed = statement.trim();
                            if (trimmed.isEmpty()) {
                                continue;
                            }
                            while (trimmed.startsWith("--")) {
                                int newlineIndex = trimmed.indexOf('\n');
                                if (newlineIndex == -1) {
                                    trimmed = "";
                                    break;
                                }
                                trimmed = trimmed.substring(newlineIndex + 1).trim();
                            }
                            if (!trimmed.isEmpty()) {
                                try (java.sql.Statement stmt = conn.createStatement()) {
                                    stmt.execute(trimmed);
                                }
                            }
                        }
                    }
                }
            }
            dataSource.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run database migrations", e);
        }
    }
}
