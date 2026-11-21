package com.example.DunbarHorizon.buzz.adapter.out.persistence.legacy;

import com.example.DunbarHorizon.support.JpaRepositoryTest;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({TestContainerConfig.class, LegacyBuzzMigrator.class})
@ImportAutoConfiguration({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, JacksonAutoConfiguration.class})
class LegacyBuzzMigratorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);
    private static final Long CREATOR_ID = 25_001L;

    @Autowired private LegacyBuzzMigrator legacyBuzzMigrator;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("buzzes");
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection("buzzes");
        jdbcTemplate.update("DELETE FROM buzz_audience_members WHERE buzz_id IN "
                + "(SELECT id FROM buzzes WHERE creator_id = ?)", CREATOR_ID);
        jdbcTemplate.update("DELETE FROM buzzes WHERE creator_id = ?", CREATOR_ID);
    }

    @Test
    @DisplayName("미만료 버즈는 원래 ID·생성·만료 시각과 공개 대상·읽음 상태를 그대로 옮기고 Mongo에서 지운다")
    void migrate_PreservesIdTimesAudienceAndRead() {
        // given
        ObjectId id = insertLegacyBuzz(NOW.minusHours(1), List.of(2L, 3L), List.of(3L),
                List.of("https://bucket.s3.ap-northeast-2.amazonaws.com/buzz/photo-1?X-Amz-Signature=abc", "buzz/photo-2"));

        // when
        int migrated = legacyBuzzMigrator.migrate(NOW);

        // then
        assertThat(migrated).isEqualTo(1);
        Map<String, Object> buzz = jdbcTemplate.queryForMap(
                "SELECT creator_id, text, CAST(image_keys AS CHAR) AS image_keys, created_at, expires_at "
                        + "FROM buzzes WHERE id = ?", id.toHexString());
        assertThat(((Number) buzz.get("creator_id")).longValue()).isEqualTo(CREATOR_ID);
        assertThat(buzz.get("image_keys").toString()).contains("buzz/photo-1", "buzz/photo-2").doesNotContain("X-Amz-");
        assertThat(buzz.get("created_at")).isEqualTo(NOW.minusHours(1));
        assertThat(buzz.get("expires_at")).isEqualTo(NOW.plusHours(5));
        assertThat(jdbcTemplate.queryForList(
                "SELECT user_id, is_read FROM buzz_audience_members WHERE buzz_id = ? ORDER BY user_id",
                id.toHexString()))
                .extracting(row -> ((Number) row.get("user_id")).longValue(), row -> row.get("is_read"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, false),
                        org.assertj.core.groups.Tuple.tuple(3L, true));
        assertThat(mongoTemplate.getCollection("buzzes").countDocuments()).isZero();
    }

    @Test
    @DisplayName("만료된 버즈와 댓글은 옮기지 않는다")
    void migrate_SkipsExpiredAndComments() {
        // given
        ObjectId expired = insertLegacyBuzz(NOW.minusHours(7), List.of(2L), List.of(), List.of());
        ObjectId live = insertLegacyBuzz(NOW.minusHours(1), List.of(2L), List.of(), List.of());

        // when
        int migrated = legacyBuzzMigrator.migrate(NOW);

        // then
        assertThat(migrated).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM buzzes WHERE id = ?", expired.toHexString())).isZero();
        assertThat(count("SELECT COUNT(*) FROM buzz_threads WHERE buzz_id = ?", live.toHexString())).isZero();
    }

    @Test
    @DisplayName("다시 실행해도 이미 옮긴 버즈를 중복하거나 게시자가 지운 버즈를 되살리지 않는다")
    void migrate_RerunDoesNotDuplicateOrResurrect() {
        // given
        ObjectId id = insertLegacyBuzz(NOW.minusHours(1), List.of(2L), List.of(), List.of());
        legacyBuzzMigrator.migrate(NOW);
        jdbcTemplate.update("DELETE FROM buzz_audience_members WHERE buzz_id = ?", id.toHexString());
        jdbcTemplate.update("DELETE FROM buzzes WHERE id = ?", id.toHexString());

        // when
        int migrated = legacyBuzzMigrator.migrate(NOW);

        // then
        assertThat(migrated).isZero();
        assertThat(count("SELECT COUNT(*) FROM buzzes WHERE id = ?", id.toHexString())).isZero();
    }

    @Test
    @DisplayName("MySQL에 이미 같은 ID가 있으면 건너뛰고 Mongo 문서만 지운다")
    void migrate_ExistingIdSkipped() {
        // given
        ObjectId id = insertLegacyBuzz(NOW.minusHours(1), List.of(2L), List.of(), List.of());
        jdbcTemplate.update("INSERT INTO buzzes (id, creator_id, text, image_keys, created_at, expires_at) "
                        + "VALUES (?, ?, '이미 옮김', JSON_ARRAY(), ?, ?)",
                id.toHexString(), CREATOR_ID, Timestamp.valueOf(NOW.minusHours(1)), Timestamp.valueOf(NOW.plusHours(5)));

        // when
        int migrated = legacyBuzzMigrator.migrate(NOW);

        // then
        assertThat(migrated).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT text FROM buzzes WHERE id = ?", String.class, id.toHexString()))
                .isEqualTo("이미 옮김");
        assertThat(count("SELECT COUNT(*) FROM buzz_audience_members WHERE buzz_id = ?", id.toHexString())).isZero();
        assertThat(mongoTemplate.getCollection("buzzes").countDocuments()).isZero();
    }

    private ObjectId insertLegacyBuzz(LocalDateTime createdAt, List<Long> recipientIds, List<Long> readRecipientIds,
                                      List<String> imageUrls) {
        ObjectId id = new ObjectId();
        mongoTemplate.insert(new Document()
                .append("_id", id)
                .append("creatorId", CREATOR_ID)
                .append("creatorNickname", "작성자")
                .append("creatorProfileImageUrl", "https://bucket/profile?X-Amz-Signature=x")
                .append("text", "옛 버즈")
                .append("imageUrls", imageUrls)
                .append("recipientIds", recipientIds)
                .append("readRecipientIds", readRecipientIds)
                .append("comments", List.of(new Document("commentId", "c1").append("text", "옛 댓글")))
                .append("createdAt", toDate(createdAt))
                .append("expiresAt", toDate(createdAt.plusHours(6))), "buzzes");
        return id;
    }

    private long count(String sql, String buzzId) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, buzzId);
        return count == null ? 0 : count;
    }

    private static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.truncatedTo(ChronoUnit.MILLIS).atZone(ZoneId.systemDefault()).toInstant());
    }
}
