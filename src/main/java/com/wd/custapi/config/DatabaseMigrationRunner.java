package com.wd.custapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting database migration check...");

        try {
            // 1. Check if project_uuid column exists
            String checkColumnSql = "SELECT count(*) FROM information_schema.columns " +
                    "WHERE table_name = 'customer_projects' AND column_name = 'project_uuid'";
            Integer count = jdbcTemplate.queryForObject(checkColumnSql, Integer.class);

            if (count != null && count == 0) {
                // Check if old public_id column exists
                String checkOldColumnSql = "SELECT count(*) FROM information_schema.columns " +
                        "WHERE table_name = 'customer_projects' AND column_name = 'public_id'";
                Integer oldCount = jdbcTemplate.queryForObject(checkOldColumnSql, Integer.class);

                if (oldCount != null && oldCount > 0) {
                    logger.info("Renaming 'public_id' to 'project_uuid'...");
                    jdbcTemplate.execute("ALTER TABLE customer_projects RENAME COLUMN public_id TO project_uuid");
                    logger.info("Column renamed.");
                } else {
                    logger.info("Column 'project_uuid' missing. Adding it...");
                    jdbcTemplate.execute("ALTER TABLE customer_projects ADD COLUMN project_uuid VARCHAR(36)");
                    logger.info("Column 'project_uuid' added.");
                }
            } else {
                logger.info("Column 'project_uuid' already exists.");
            }

            // 2. Backfill UUIDs for existing rows where project_uuid is null
            String findNullsSql = "SELECT id FROM customer_projects WHERE project_uuid IS NULL";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(findNullsSql);

            if (!rows.isEmpty()) {
                logger.info("Found {} rows with null project_uuid. Updating...", rows.size());
                for (Map<String, Object> row : rows) {
                    Long id = (Long) row.get("id");
                    String uuid = UUID.randomUUID().toString();
                    jdbcTemplate.update("UPDATE customer_projects SET project_uuid = ? WHERE id = ?", uuid, id);
                }
                logger.info("Successfully updated project_uuid for existing rows.");
            } else {
                logger.info("No rows found with null project_uuid.");
            }

            // 3. Add Unique Constraint
            String checkConstraintSql = "SELECT count(*) FROM information_schema.table_constraints " +
                    "WHERE table_name = 'customer_projects' AND constraint_name = 'uk_project_uuid'";
            Integer constraintCount = jdbcTemplate.queryForObject(checkConstraintSql, Integer.class);

            if (constraintCount != null && constraintCount == 0) {
                logger.info("Adding unique constraint 'uk_project_uuid'...");
                try {
                    jdbcTemplate.execute(
                            "ALTER TABLE customer_projects ADD CONSTRAINT uk_project_uuid UNIQUE (project_uuid)");
                    logger.info("Unique constraint added.");
                } catch (Exception e) {
                    logger.warn("Failed to add unique constraint (might already exist or data issue): {}",
                            e.getMessage());
                }
            } else {
                logger.info("Unique constraint 'uk_project_uuid' already exists.");
            }

            // 4. Set Not Null
            try {
                jdbcTemplate.execute("ALTER TABLE customer_projects ALTER COLUMN project_uuid SET NOT NULL");
                logger.info("Set project_uuid to NOT NULL.");
            } catch (Exception e) {
                logger.warn("Could not set project_uuid to NOT NULL: {}", e.getMessage());
            }

            // 5. Add sqfeet column if missing
            String checkSqFeetSql = "SELECT count(*) FROM information_schema.columns " +
                    "WHERE table_name = 'customer_projects' AND column_name = 'sqfeet'";
            Integer sqFeetCount = jdbcTemplate.queryForObject(checkSqFeetSql, Integer.class);

            if (sqFeetCount != null && sqFeetCount == 0) {
                logger.info("Column 'sqfeet' missing. Adding it...");
                jdbcTemplate.execute("ALTER TABLE customer_projects ADD COLUMN sqfeet DOUBLE PRECISION");
                logger.info("Column 'sqfeet' added.");
            } else {
                logger.info("Column 'sqfeet' already exists.");
            }

            // 6. Add GPS distance tracking columns to site_visits
            addColumnIfMissing("site_visits", "distance_from_project_checkin", "DOUBLE PRECISION");
            addColumnIfMissing("site_visits", "distance_from_project_checkout", "DOUBLE PRECISION");

            // 7. Create password reset tokens table
            createPasswordResetTokensTable();

        } catch (Exception e) {
            logger.error("Database migration failed: ", e);
        }

        logger.info("Database migration check completed.");
    }

    /**
     * Create password reset tokens table if it doesn't exist.
     */
    private void createPasswordResetTokensTable() {
        try {
            String checkSql = "SELECT count(*) FROM information_schema.tables " +
                    "WHERE table_name = 'customer_password_reset_tokens'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (count != null && count == 0) {
                logger.info("Table 'customer_password_reset_tokens' missing. Creating it...");
                jdbcTemplate.execute(
                    "CREATE TABLE customer_password_reset_tokens (" +
                    "  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY," +
                    "  email VARCHAR(255) NOT NULL," +
                    "  reset_code VARCHAR(10) NOT NULL," +
                    "  expires_at TIMESTAMP NOT NULL," +
                    "  used BOOLEAN DEFAULT FALSE," +
                    "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")"
                );
                logger.info("Table 'customer_password_reset_tokens' created.");
            } else {
                logger.info("Table 'customer_password_reset_tokens' already exists.");
            }
        } catch (Exception e) {
            logger.warn("Failed to create password reset tokens table: {}", e.getMessage());
        }
    }

    /**
     * Helper to add a column to a table if it doesn't already exist.
     */
    private void addColumnIfMissing(String tableName, String columnName, String columnType) {
        try {
            String checkSql = "SELECT count(*) FROM information_schema.columns " +
                    "WHERE table_name = '" + tableName + "' AND column_name = '" + columnName + "'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (count != null && count == 0) {
                logger.info("Column '{}' missing from '{}'. Adding it...", columnName, tableName);
                jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
                logger.info("Column '{}' added to '{}'.", columnName, tableName);
            } else {
                logger.info("Column '{}' already exists in '{}'.", columnName, tableName);
            }
        } catch (Exception e) {
            logger.warn("Failed to add column '{}' to '{}': {}", columnName, tableName, e.getMessage());
        }
    }
}
