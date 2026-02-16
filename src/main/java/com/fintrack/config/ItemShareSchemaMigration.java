package com.fintrack.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time migration: allows {@code item_shares.user_id} to be NULL so shares can be assigned
 * to trusted contacts (not only system users). Runs after startup when using MySQL; no-op in tests.
 */
@Component
@Profile("!test")
@Order(Integer.MAX_VALUE)
public class ItemShareSchemaMigration implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ItemShareSchemaMigration.class);

    private static final String CHECK_NULLABLE_SQL =
            "SELECT CASE WHEN IS_NULLABLE = 'YES' THEN 1 ELSE 0 END FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'item_shares' AND COLUMN_NAME = 'user_id'";
    private static final String ALTER_USER_ID_NULLABLE_SQL =
            "ALTER TABLE item_shares MODIFY COLUMN user_id BIGINT NULL";

    private final JdbcTemplate jdbcTemplate;
    private final String datasourceUrl;

    public ItemShareSchemaMigration(final DataSource dataSource,
                                   @Value("${spring.datasource.url:}") final String datasourceUrl) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.datasourceUrl = datasourceUrl != null ? datasourceUrl : "";
    }

    @Override
    public void run(final ApplicationArguments args) {
        if (!isMySql()) {
            return;
        }
        if (!isUserIdColumnNotNull()) {
            return;
        }
        try {
            jdbcTemplate.execute(ALTER_USER_ID_NULLABLE_SQL);
            LOG.info("Migration applied: item_shares.user_id is now nullable (trusted contact support).");
        } catch (Exception e) {
            LOG.debug("Item share user_id migration skipped or failed (table/column may not exist yet): {}",
                    e.getMessage());
        }
    }

    private boolean isMySql() {
        return datasourceUrl.toLowerCase().contains("mysql");
    }

    private boolean isUserIdColumnNotNull() {
        try {
            Integer nullable = jdbcTemplate.queryForObject(CHECK_NULLABLE_SQL, Integer.class);
            return nullable != null && nullable == 0;
        } catch (Exception e) {
            LOG.debug("Could not check item_shares.user_id nullable status: {}", e.getMessage());
            return false;
        }
    }
}
