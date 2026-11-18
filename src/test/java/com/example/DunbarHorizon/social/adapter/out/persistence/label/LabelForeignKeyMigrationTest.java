package com.example.DunbarHorizon.social.adapter.out.persistence.label;

import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LabelForeignKeyMigrationTest {

    @Autowired private MySQLContainer<?> mysqlContainer;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("신규 DB의 전체 마이그레이션과 기존 데이터의 외래 키 제거가 같은 스키마를 만든다")
    void migrate_PreservesDataAndIndexes_ExistingLabels() {
        // given
        assertThat(foreignKeyCount(jdbcTemplate)).isZero();
        String databaseName = "label_migration_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String jdbcUrl = mysqlContainer.getJdbcUrl()
                .replace("/" + mysqlContainer.getDatabaseName(), "/" + databaseName);
        var dataSource = new DriverManagerDataSource(jdbcUrl, mysqlContainer.getUsername(), mysqlContainer.getPassword());
        var migrationJdbc = new JdbcTemplate(dataSource);
        try {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("10").load().migrate();
            migrationJdbc.update("INSERT INTO users (user_id, email, nickname, role, status) VALUES (1, 'owner@migration.test', 'owner', 'USER', 'ACTIVE')");
            migrationJdbc.update("INSERT INTO users (user_id, email, nickname, role, status) VALUES (2, 'member@migration.test', 'member', 'USER', 'ACTIVE')");
            migrationJdbc.update("INSERT INTO labels (id, owner_id, name) VALUES ('label', 1, '친구')");
            migrationJdbc.update("INSERT INTO label_members (label_id, member_id) VALUES ('label', 2)");
            assertThat(foreignKeyCount(migrationJdbc)).isEqualTo(3);
            List<Map<String, Object>> labels = migrationJdbc.queryForList("SELECT * FROM labels ORDER BY id");
            List<Map<String, Object>> members = migrationJdbc.queryForList("SELECT * FROM label_members ORDER BY label_id, member_id");
            List<Map<String, Object>> indexes = indexes(migrationJdbc);

            // when
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("11").load().migrate();

            // then
            assertThat(foreignKeyCount(migrationJdbc)).isZero();
            assertThat(migrationJdbc.queryForList("SELECT * FROM labels ORDER BY id")).isEqualTo(labels);
            assertThat(migrationJdbc.queryForList("SELECT * FROM label_members ORDER BY label_id, member_id")).isEqualTo(members);
            assertThat(indexes(migrationJdbc)).isEqualTo(indexes);
            assertThatThrownBy(() -> migrationJdbc.update("INSERT INTO labels (id, owner_id, name) VALUES ('duplicate', 1, '친구')"))
                    .isInstanceOf(DuplicateKeyException.class);
            assertThatThrownBy(() -> migrationJdbc.update("INSERT INTO label_members (label_id, member_id) VALUES ('label', 2)"))
                    .isInstanceOf(DuplicateKeyException.class);
            migrationJdbc.update("DELETE FROM labels WHERE id = 'label'");
            assertThat(migrationJdbc.queryForObject("SELECT COUNT(*) FROM label_members", Long.class)).isEqualTo(1);
        } finally {
            jdbcTemplate.execute("DROP DATABASE " + databaseName);
        }
    }

    private long foreignKeyCount(JdbcTemplate jdbc) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema = DATABASE() AND table_name IN ('labels', 'label_members')
                  AND constraint_type = 'FOREIGN KEY'
                """, Long.class);
    }

    private List<Map<String, Object>> indexes(JdbcTemplate jdbc) {
        return jdbc.queryForList("""
                SELECT table_name, index_name, non_unique, seq_in_index, column_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name IN ('labels', 'label_members')
                ORDER BY table_name, index_name, seq_in_index
                """);
    }
}
