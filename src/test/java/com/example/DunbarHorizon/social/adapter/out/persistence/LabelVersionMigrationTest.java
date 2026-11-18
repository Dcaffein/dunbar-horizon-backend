package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LabelVersionMigrationTest {

    @Autowired private MySQLContainer<?> mysqlContainer;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("버전 열 추가 전에 있던 라벨은 버전 0을 가진다")
    void migrate_AssignsInitialVersion_ExistingLabels() {
        // given
        String databaseName = "label_version_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String jdbcUrl = mysqlContainer.getJdbcUrl()
                .replace("/" + mysqlContainer.getDatabaseName(), "/" + databaseName);
        var dataSource = new DriverManagerDataSource(jdbcUrl, mysqlContainer.getUsername(), mysqlContainer.getPassword());
        var migrationJdbc = new JdbcTemplate(dataSource);
        try {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("11").load().migrate();
            migrationJdbc.update("INSERT INTO users (user_id, email, nickname, role, status) VALUES (1, 'owner@version.test', 'owner', 'USER', 'ACTIVE')");
            migrationJdbc.update("INSERT INTO labels (id, owner_id, name) VALUES ('label', 1, '친구')");

            // when
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

            // then
            assertThat(migrationJdbc.queryForObject("SELECT version FROM labels WHERE id = 'label'", Long.class)).isZero();
        } finally {
            jdbcTemplate.execute("DROP DATABASE " + databaseName);
        }
    }
}
