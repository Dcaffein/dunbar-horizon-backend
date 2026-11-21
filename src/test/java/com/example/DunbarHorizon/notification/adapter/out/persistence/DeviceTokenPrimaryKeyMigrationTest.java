package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeviceTokenPrimaryKeyMigrationTest {

    @Autowired private JdbcConnectionDetails connectionDetails;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("UNIQUE 인덱스 이름이 기준선과 같으면 토큰이 기본 키가 되고 기존 행이 남는다")
    void migrate_TokenBecomesPrimaryKey_BaselineIndexName() {
        migrateAndAssert(migrationJdbc -> { });
    }

    @Test
    @DisplayName("UNIQUE 인덱스 이름이 기준선과 달라도 그 인덱스를 지우고 토큰이 기본 키가 된다")
    void migrate_TokenBecomesPrimaryKey_DifferentIndexName() {
        migrateAndAssert(migrationJdbc -> migrationJdbc.execute(
                "ALTER TABLE device_tokens RENAME INDEX UK3shgoehod3h5jkmwsgkxora0u TO UK_legacy_fcm_token"));
    }

    private void migrateAndAssert(Consumer<JdbcTemplate> beforeV14) {
        String databaseName = "device_token_pk_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        try {
            // given
            var dataSource = new DriverManagerDataSource(
                    connectionDetails.getJdbcUrl(), connectionDetails.getUsername(), connectionDetails.getPassword());
            dataSource.setCatalog(databaseName);
            var migrationJdbc = new JdbcTemplate(dataSource);
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("13").load().migrate();
            migrationJdbc.update("INSERT INTO device_tokens (user_id, fcm_token) VALUES (1, 'token-a'), (1, 'token-b')");
            beforeV14.accept(migrationJdbc);

            // when
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

            // then
            assertThat(migrationJdbc.queryForList("""
                    SELECT INDEX_NAME, COLUMN_NAME FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device_tokens' AND NON_UNIQUE = 0
                    """))
                    .containsExactly(Map.of("INDEX_NAME", "PRIMARY", "COLUMN_NAME", "token"));
            assertThat(migrationJdbc.queryForList("SELECT token FROM device_tokens WHERE user_id = 1", String.class))
                    .containsExactlyInAnyOrder("token-a", "token-b");
        } finally {
            jdbcTemplate.execute("DROP DATABASE " + databaseName);
        }
    }
}
