package com.example.order.config;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Applies the order-service Flyway migration scripts on startup.
 *
 * <p>{@code FlywayAutoConfiguration} is intentionally excluded from this application because the
 * bundled Flyway version does not support PostgreSQL 16 (used in production). Until Flyway is
 * upgraded, migrations are applied directly here, mirroring {@code TestDatabaseMigrations} which
 * performs the same step in tests.
 *
 * <p>The runner is a no-op when the schema is already initialized (detected via the {@code orders}
 * table) so it is safe to run on every startup, and it is disabled under the {@code test} profile
 * where {@code TestDatabaseMigrations} already provisions the Testcontainers database.
 */
@Component
@Profile("!test")
public class DatabaseMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private static final String[] MIGRATION_SCRIPTS = {
        "db/migration/V1__create_order_table.sql",
        "db/migration/V2__create_outbox_table.sql",
        "db/migration/V3__create_processed_events_table.sql",
        "db/migration/V4__create_shedlock_table.sql",
        "db/migration/V5__create_order_item_table.sql",
        "db/migration/V6__create_shipment_tables.sql"
    };

    private final DataSource dataSource;

    public DatabaseMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            // Skip when the schema is already initialized (idempotent across restarts).
            try (Statement check = conn.createStatement();
                 ResultSet rs = check.executeQuery("SELECT to_regclass('public.orders')")) {
                if (rs.next() && rs.getObject(1) != null) {
                    log.debug("Database schema already initialized, skipping migrations");
                    return;
                }
            }

            for (String script : MIGRATION_SCRIPTS) {
                log.info("Running database migration: {}", script);
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(script)) {
                    if (is == null) {
                        throw new IllegalStateException("Migration script not found: " + script);
                    }
                    String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
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
                            try (Statement stmt = conn.createStatement()) {
                                stmt.execute(trimmed);
                            }
                        }
                    }
                }
            }
            log.info("Database migrations completed");
        } catch (SQLException | IOException e) {
            // Never crash startup because of migrations; a missing schema surfaces as a clear
            // SQL error on the first request instead of an opaque context failure.
            log.warn("Could not initialize database schema on startup: {}", e.getMessage());
        }
    }
}
