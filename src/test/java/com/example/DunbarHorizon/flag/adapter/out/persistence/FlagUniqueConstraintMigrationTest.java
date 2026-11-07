package com.example.DunbarHorizon.flag.adapter.out.persistence;

import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공유 스키마에는 이미 V4가 적용돼 있어 중복 행을 넣을 수 없다.
 * 별도 스키마를 V3까지 올려 V4 적용 전 상태를 만든다.
 */
@JpaRepositoryTest
class FlagUniqueConstraintMigrationTest {

    private static final String SCHEMA = "flag_v4_migration_check";

    @Autowired private MySQLContainer<?> mysql;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
        jdbcTemplate.execute("DROP DATABASE IF EXISTS " + SCHEMA);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DROP DATABASE IF EXISTS " + SCHEMA);
    }

    @Test
    @DisplayName("중복 행이 있어도 V4가 성공하고 가장 먼저 만든 행만 남는다")
    void migrate_RemovesDuplicatesAndKeepsFirstRow() {
        // given
        flyway().target("3").load().migrate();

        jdbcTemplate.update("INSERT INTO " + SCHEMA + ".flag_invitations (id, flag_id, invitee_id, inviter_id) VALUES "
                + "(1, 100, 200, 300), (2, 100, 200, 301), (3, 100, 201, 300)");
        jdbcTemplate.update("INSERT INTO " + SCHEMA + ".flag_participants (id, flag_id, participant_id, can_invite) VALUES "
                + "(1, 100, 200, 0), (2, 100, 200, 1), (3, 100, 201, 0)");

        // when
        flyway().load().migrate();

        // then
        assertThat(idsOf("flag_invitations")).containsExactly(1L, 3L);
        assertThat(idsOf("flag_participants")).containsExactly(1L, 3L);
        assertThat(uniqueIndexNames()).contains(
                "uq_flag_invitations_flag_invitee", "uq_flag_participants_flag_participant");
    }

    private FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .createSchemas(true)
                .locations("classpath:db/migration");
    }

    private List<Long> idsOf(String table) {
        return jdbcTemplate.queryForList("SELECT id FROM " + SCHEMA + "." + table + " ORDER BY id", Long.class);
    }

    private List<String> uniqueIndexNames() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT index_name FROM information_schema.statistics "
                        + "WHERE table_schema = ? AND non_unique = 0",
                String.class, SCHEMA);
    }
}
