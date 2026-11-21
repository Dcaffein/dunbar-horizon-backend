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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationReceiverRequiredMigrationTest {

    @Autowired private JdbcConnectionDetails connectionDetails;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("수신자가 없는 알림을 지우고 수신자 열을 필수로 바꾼다")
    void migrate_DeletesReceiverlessRowsAndRequiresReceiver() {
        String databaseName = "notification_receiver_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        try {
            // given
            var dataSource = new DriverManagerDataSource(
                    connectionDetails.getJdbcUrl(), connectionDetails.getUsername(), connectionDetails.getPassword());
            dataSource.setCatalog(databaseName);
            var migrationJdbc = new JdbcTemplate(dataSource);
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("14").load().migrate();
            migrationJdbc.update("""
                    INSERT INTO notifications (receiver_id, title, content, type, is_read, is_sent, created_at)
                    VALUES (1, '내 알림', '내용', 'BUZZ_ARRIVAL', false, false, NOW(6)),
                           (NULL, '전체 공지', '내용', 'NOTICE', false, true, NOW(6))
                    """);

            // when
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

            // then
            assertThat(migrationJdbc.queryForList("SELECT title FROM notifications", String.class))
                    .containsExactly("내 알림");
            assertThat(migrationJdbc.queryForObject("""
                    SELECT IS_NULLABLE FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'receiver_id'
                    """, String.class)).isEqualTo("NO");
        } finally {
            jdbcTemplate.execute("DROP DATABASE " + databaseName);
        }
    }
}
