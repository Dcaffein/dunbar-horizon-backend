package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Import({BuzzRepositoryAdapter.class, BuzzAudienceMemberRepositoryAdapter.class, BuzzQueryRepositoryAdapter.class})
class BuzzPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    @Autowired private BuzzRepositoryAdapter buzzRepositoryAdapter;
    @Autowired private BuzzAudienceMemberRepositoryAdapter buzzAudienceMemberRepositoryAdapter;
    @Autowired private BuzzQueryRepositoryAdapter buzzQueryRepositoryAdapter;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final Long creatorId = 1L;
    private final Long otherCreatorId = 5L;
    private final Long recipientId = 2L;

    private Buzz saveBuzz(Long creator, LocalDateTime createdAt, Long... audienceIds) {
        Buzz buzz = Buzz.create(creator, "본문", List.of("buzz/a"), Arrays.asList(audienceIds), createdAt);
        buzzRepositoryAdapter.save(buzz);
        buzzAudienceMemberRepositoryAdapter.saveAll(Arrays.stream(audienceIds)
                .map(userId -> BuzzAudienceMember.create(buzz.getId(), userId))
                .toList());
        entityManager.flush();
        entityManager.clear();
        return buzz;
    }

    @Test
    @DisplayName("저장한 버즈의 이미지 키는 서명되지 않은 키 그대로 남는다")
    void save_StoresImageKeysWithoutSignature() {
        // given
        Buzz buzz = saveBuzz(creatorId, NOW, recipientId);

        // when
        String stored = jdbcTemplate.queryForObject(
                "SELECT CAST(image_keys AS CHAR) FROM buzzes WHERE id = ?", String.class, buzz.getId());

        // then
        assertThat(stored).contains("buzz/a").doesNotContain("X-Amz-");
        assertThat(buzzRepositoryAdapter.findById(buzz.getId()).orElseThrow().getImageKeys())
                .containsExactly("buzz/a");
    }

    @Test
    @DisplayName("받은 목록은 만료되지 않았고 제외 대상이 아닌 게시자의 버즈만 최신순으로 읽음 상태와 함께 준다")
    void findReceived_FiltersExpiredAndExcluded() {
        // given
        Buzz older = saveBuzz(creatorId, NOW.minusHours(2), recipientId);
        Buzz newer = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        saveBuzz(creatorId, NOW.minusHours(6), recipientId);
        saveBuzz(otherCreatorId, NOW.minusHours(1), recipientId);
        saveBuzz(creatorId, NOW.minusHours(1), 3L);
        readBuzz(older.getId(), recipientId);

        // when
        Slice<ReceivedBuzz> result = buzzQueryRepositoryAdapter.findReceived(
                recipientId, Set.of(otherCreatorId), NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(received -> received.buzz().getId())
                .containsExactly(newer.getId(), older.getId());
        assertThat(result.getContent()).extracting(ReceivedBuzz::read).containsExactly(false, true);
    }

    @Test
    @DisplayName("제외할 게시자가 없어도 받은 목록을 조회한다")
    void findReceived_NoExcludedCreators() {
        // given
        saveBuzz(creatorId, NOW.minusHours(1), recipientId);

        // when
        Slice<ReceivedBuzz> result = buzzQueryRepositoryAdapter.findReceived(
                recipientId, Set.of(), NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("만료 시각이 기준 시각과 같은 버즈는 받은 목록에서 빠진다")
    void findReceived_ExcludesExpiresAtEqualToNow() {
        // given
        saveBuzz(creatorId, NOW.minusHours(6), recipientId);

        // when
        Slice<ReceivedBuzz> result = buzzQueryRepositoryAdapter.findReceived(
                recipientId, Set.of(), NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("보낸 목록은 만료되지 않은 자기 버즈만 준다")
    void findSent_FiltersExpired() {
        // given
        Buzz live = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        saveBuzz(creatorId, NOW.minusHours(7), recipientId);
        saveBuzz(otherCreatorId, NOW.minusHours(1), recipientId);

        // when
        Slice<Buzz> result = buzzQueryRepositoryAdapter.findSent(creatorId, NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Buzz::getId).containsExactly(live.getId());
    }

    @Test
    @DisplayName("미확인 작성자는 읽지 않았고 만료되지 않았으며 제외 대상이 아닌 버즈의 게시자다")
    void findUnreadCreatorIds_Filters() {
        // given
        saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        Buzz read = saveBuzz(10L, NOW.minusHours(1), recipientId);
        readBuzz(read.getId(), recipientId);
        saveBuzz(11L, NOW.minusHours(7), recipientId);
        saveBuzz(otherCreatorId, NOW.minusHours(1), recipientId);

        // when
        List<Long> result = buzzQueryRepositoryAdapter.findUnreadCreatorIds(
                recipientId, Set.of(otherCreatorId), NOW);

        // then
        assertThat(result).containsExactly(creatorId);
    }

    @Test
    @DisplayName("멤버가 읽으면 그 멤버의 행만 바뀐다")
    void read_ChangesOnlyThatMember() {
        // given
        Buzz buzz = saveBuzz(creatorId, NOW, recipientId, 3L);

        // when
        readBuzz(buzz.getId(), recipientId);

        // then
        assertThat(readStates(buzz.getId())).isEqualTo(java.util.Map.of(recipientId, true, 3L, false));
    }

    @Test
    @DisplayName("버즈들과 그 멤버를 지우면 다른 버즈의 행은 남는다")
    void deleteAllByIds_RemovesOnlyTargets() {
        // given
        Buzz target = saveBuzz(creatorId, NOW, recipientId, 3L);
        Buzz other = saveBuzz(creatorId, NOW, recipientId);

        // when
        buzzRepositoryAdapter.deleteAllByIds(List.of(target.getId()));
        buzzAudienceMemberRepositoryAdapter.deleteAllByBuzzIds(List.of(target.getId()));

        // then
        assertThat(count("SELECT COUNT(*) FROM buzzes WHERE id = ?", target.getId())).isZero();
        assertThat(buzzAudienceMemberRepositoryAdapter.findUserIdsByBuzzId(target.getId())).isEmpty();
        assertThat(buzzAudienceMemberRepositoryAdapter.findUserIdsByBuzzId(other.getId())).containsExactly(recipientId);
    }

    @Test
    @DisplayName("삭제 표시된 버즈는 단건·잠금 조회와 받은 목록·보낸 목록·미확인 작성자에서 빠진다")
    void deletedBuzz_ExcludedFromAllReads() {
        // given
        Buzz deleted = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        markDeleted(deleted.getId());

        // when & then
        assertThat(buzzRepositoryAdapter.findById(deleted.getId())).isEmpty();
        assertThat(buzzRepositoryAdapter.findByIdForShare(deleted.getId())).isEmpty();
        assertThat(buzzRepositoryAdapter.findByIdForUpdate(deleted.getId())).isEmpty();
        assertThat(buzzQueryRepositoryAdapter.findReceived(recipientId, Set.of(), NOW, PageRequest.of(0, 20))
                .getContent()).isEmpty();
        assertThat(buzzQueryRepositoryAdapter.findSent(creatorId, NOW, PageRequest.of(0, 20)).getContent()).isEmpty();
        assertThat(buzzQueryRepositoryAdapter.findUnreadCreatorIds(recipientId, Set.of(), NOW)).isEmpty();
    }

    @Test
    @DisplayName("정리 대상은 만료 시각이 기준 시각 이전이거나 같은 버즈와 삭제 표시된 버즈다")
    void findPurgeableIds_ExpiredOrDeleted() {
        // given
        Buzz expired = saveBuzz(creatorId, NOW.minusHours(6), recipientId);
        Buzz deleted = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        markDeleted(deleted.getId());
        Buzz live = saveBuzz(creatorId, NOW.minusHours(1), recipientId);

        // when
        List<Long> purgeable = buzzRepositoryAdapter.findPurgeableIds(NOW);

        // then
        assertThat(purgeable).contains(expired.getId(), deleted.getId()).doesNotContain(live.getId());
    }

    @Test
    @DisplayName("삭제는 삭제 시각만 쓰고 본문은 그대로 둔다")
    void delete_WritesOnlyDeletedAt() {
        // given
        Buzz saved = saveBuzz(creatorId, NOW, recipientId);
        Buzz loaded = buzzRepositoryAdapter.findByIdForUpdate(saved.getId()).orElseThrow();

        // when
        loaded.delete(creatorId, NOW.plusMinutes(5));
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT text FROM buzzes WHERE id = ?", String.class, saved.getId()))
                .isEqualTo("본문");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM buzzes WHERE id = ?", Boolean.class, saved.getId())).isTrue();
    }

    @Test
    @DisplayName("고친 본문·이미지 키와 수정 시각이 저장된다")
    void update_PersistsChangesAndEditedAt() {
        // given
        Buzz saved = saveBuzz(creatorId, NOW, recipientId);
        Buzz loaded = buzzRepositoryAdapter.findById(saved.getId()).orElseThrow();

        // when
        loaded.update(creatorId, "고친 본문", List.of("buzz/b"), NOW.plusMinutes(10));
        entityManager.flush();
        entityManager.clear();

        // then
        Buzz reloaded = buzzRepositoryAdapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getText()).isEqualTo("고친 본문");
        assertThat(reloaded.getImageKeys()).containsExactly("buzz/b");
        assertThat(reloaded.getEditedAt()).isEqualTo(NOW.plusMinutes(10));
        assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
    }

    private void markDeleted(Long buzzId) {
        jdbcTemplate.update("UPDATE buzzes SET deleted_at = ? WHERE id = ?", NOW, buzzId);
        entityManager.clear();
    }

    private long count(String sql, Long buzzId) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, buzzId);
        return count == null ? 0 : count;
    }

    private void readBuzz(Long buzzId, Long userId) {
        buzzAudienceMemberRepositoryAdapter.findByBuzzIdAndUserId(buzzId, userId).orElseThrow().read();
        entityManager.flush();
        entityManager.clear();
    }

    private java.util.Map<Long, Boolean> readStates(Long buzzId) {
        return jdbcTemplate.queryForList(
                        "SELECT user_id, is_read FROM buzz_audience_members WHERE buzz_id = ?", buzzId).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("user_id")).longValue(),
                        row -> (Boolean) row.get("is_read")));
    }
}
