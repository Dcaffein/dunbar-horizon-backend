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
    void findUnexpiredReceived_FiltersExpiredAndExcluded() {
        // given
        Buzz older = saveBuzz(creatorId, NOW.minusHours(2), recipientId);
        Buzz newer = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        saveBuzz(creatorId, NOW.minusHours(6), recipientId);
        saveBuzz(otherCreatorId, NOW.minusHours(1), recipientId);
        saveBuzz(creatorId, NOW.minusHours(1), 3L);
        markAsRead(older.getId(), recipientId);

        // when
        Slice<ReceivedBuzz> result = buzzQueryRepositoryAdapter.findUnexpiredReceived(
                recipientId, Set.of(otherCreatorId), NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(received -> received.buzz().getId())
                .containsExactly(newer.getId(), older.getId());
        assertThat(result.getContent()).extracting(ReceivedBuzz::read).containsExactly(false, true);
    }

    @Test
    @DisplayName("제외할 게시자가 없어도 받은 목록을 조회한다")
    void findUnexpiredReceived_NoExcludedCreators() {
        // given
        saveBuzz(creatorId, NOW.minusHours(1), recipientId);

        // when
        Slice<ReceivedBuzz> result = buzzQueryRepositoryAdapter.findUnexpiredReceived(
                recipientId, Set.of(), NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("만료 시각이 기준 시각과 같은 버즈는 받은 목록에서 빠진다")
    void findUnexpiredReceived_ExcludesExpiresAtEqualToNow() {
        // given
        saveBuzz(creatorId, NOW.minusHours(6), recipientId);

        // when
        Slice<ReceivedBuzz> result = buzzQueryRepositoryAdapter.findUnexpiredReceived(
                recipientId, Set.of(), NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("보낸 목록은 만료되지 않은 자기 버즈만 준다")
    void findUnexpiredSent_FiltersExpired() {
        // given
        Buzz live = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        saveBuzz(creatorId, NOW.minusHours(7), recipientId);
        saveBuzz(otherCreatorId, NOW.minusHours(1), recipientId);

        // when
        Slice<Buzz> result = buzzQueryRepositoryAdapter.findUnexpiredSent(creatorId, NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Buzz::getId).containsExactly(live.getId());
    }

    @Test
    @DisplayName("미확인 작성자는 읽지 않았고 만료되지 않았으며 제외 대상이 아닌 버즈의 게시자다")
    void findUnexpiredUnreadCreatorIds_Filters() {
        // given
        saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        Buzz read = saveBuzz(10L, NOW.minusHours(1), recipientId);
        markAsRead(read.getId(), recipientId);
        saveBuzz(11L, NOW.minusHours(7), recipientId);
        saveBuzz(otherCreatorId, NOW.minusHours(1), recipientId);

        // when
        List<Long> result = buzzQueryRepositoryAdapter.findUnexpiredUnreadCreatorIds(
                recipientId, Set.of(otherCreatorId), NOW);

        // then
        assertThat(result).containsExactly(creatorId);
    }

    @Test
    @DisplayName("읽음 처리는 그 멤버의 행만 바꾼다")
    void markAsRead_ChangesOnlyThatMember() {
        // given
        Buzz buzz = saveBuzz(creatorId, NOW, recipientId, 3L);

        // when
        markAsRead(buzz.getId(), recipientId);

        // then
        assertThat(readStates(buzz.getId())).isEqualTo(java.util.Map.of(recipientId, true, 3L, false));
    }

    @Test
    @DisplayName("버즈를 지우고 멤버를 지우면 그 버즈의 행만 남지 않는다")
    void delete_RemovesBuzzAndMembers() {
        // given
        Buzz target = saveBuzz(creatorId, NOW, recipientId, 3L);
        Buzz other = saveBuzz(creatorId, NOW, recipientId);

        // when
        buzzRepositoryAdapter.deleteById(target.getId());
        buzzAudienceMemberRepositoryAdapter.deleteAllByBuzzId(target.getId());

        // then
        assertThat(buzzRepositoryAdapter.findById(target.getId())).isEmpty();
        assertThat(buzzAudienceMemberRepositoryAdapter.findUserIdsByBuzzId(target.getId())).isEmpty();
        assertThat(buzzAudienceMemberRepositoryAdapter.findUserIdsByBuzzId(other.getId())).containsExactly(recipientId);
    }

    private void markAsRead(String buzzId, Long userId) {
        buzzAudienceMemberRepositoryAdapter.findByBuzzIdAndUserId(buzzId, userId).orElseThrow().markAsRead();
        entityManager.flush();
        entityManager.clear();
    }

    private java.util.Map<Long, Boolean> readStates(String buzzId) {
        return jdbcTemplate.queryForList(
                        "SELECT user_id, is_read FROM buzz_audience_members WHERE buzz_id = ?", buzzId).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("user_id")).longValue(),
                        row -> (Boolean) row.get("is_read")));
    }
}
