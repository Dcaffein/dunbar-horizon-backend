package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzAudienceInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCommentInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzSummaryInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzUserProfileInfo;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
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
        Slice<BuzzSummaryInfo> result = buzzQueryRepositoryAdapter.findReceived(
                recipientId, Set.of(otherCreatorId), NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(BuzzSummaryInfo::buzzId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(result.getContent()).extracting(BuzzSummaryInfo::isRead).containsExactly(false, true);
    }

    @Test
    @DisplayName("제외할 게시자가 없어도 받은 목록을 조회한다")
    void findReceived_NoExcludedCreators() {
        // given
        saveBuzz(creatorId, NOW.minusHours(1), recipientId);

        // when
        Slice<BuzzSummaryInfo> result = buzzQueryRepositoryAdapter.findReceived(
                recipientId, Set.of(), NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("상세는 버즈와 그 버즈의 스레드를 생성 순서대로 주고 다른 버즈의 스레드는 섞지 않는다")
    void findDetail_ThreadsInCreatedOrder() {
        // given
        Buzz buzz = saveBuzz(creatorId, NOW.minusHours(1), recipientId, 3L);
        Buzz other = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        BuzzThread later = saveThread(buzz, 3L, NOW.minusMinutes(5));
        BuzzThread earlier = saveThread(buzz, recipientId, NOW.minusMinutes(30));
        saveThread(other, recipientId, NOW.minusMinutes(10));

        // when
        BuzzDetailInfo detail = buzzQueryRepositoryAdapter.findDetail(buzz.getId(), NOW).orElseThrow();

        // then
        assertThat(detail.creatorId()).isEqualTo(creatorId);
        assertThat(detail.imageKeys()).containsExactly("buzz/a");
        assertThat(detail.expiresAt()).isEqualTo(NOW.plusHours(5));
        assertThat(detail.threads()).extracting(BuzzThreadInfo::threadId)
                .containsExactly(earlier.getId(), later.getId());
    }

    @Test
    @DisplayName("스레드가 없는 버즈도 상세를 주고 스레드는 비어 있다")
    void findDetail_NoThreads() {
        // given
        Buzz buzz = saveBuzz(creatorId, NOW.minusHours(1), recipientId);

        // when & then
        assertThat(buzzQueryRepositoryAdapter.findDetail(buzz.getId(), NOW).orElseThrow().threads()).isEmpty();
    }

    @Test
    @DisplayName("만료 시각이 기준 시각과 같거나 지난 버즈는 상세에서 빠진다")
    void findDetail_ExcludesExpired() {
        // given
        Buzz expiresNow = saveBuzz(creatorId, NOW.minusHours(6), recipientId);
        Buzz expired = saveBuzz(creatorId, NOW.minusHours(7), recipientId);

        // when & then
        assertThat(buzzQueryRepositoryAdapter.findDetail(expiresNow.getId(), NOW)).isEmpty();
        assertThat(buzzQueryRepositoryAdapter.findDetail(expired.getId(), NOW)).isEmpty();
    }

    @Test
    @DisplayName("공개 대상은 그 버즈의 멤버만 읽음 상태와 함께 준다")
    void findAudience_WithReadState() {
        // given
        Buzz buzz = saveBuzz(creatorId, NOW, recipientId, 3L);
        saveBuzz(creatorId, NOW, 4L);
        readBuzz(buzz.getId(), recipientId);

        // when & then
        assertThat(buzzQueryRepositoryAdapter.findAudience(buzz.getId()))
                .containsExactly(new BuzzAudienceInfo(recipientId, null, true), new BuzzAudienceInfo(3L, null, false));
    }

    @Test
    @DisplayName("상세는 활성 사용자인 게시자와 스레드 시작자의 프로필을 함께 주고 비활성 사용자는 프로필을 비운다")
    void findDetail_JoinsActiveProfiles() {
        // given
        insertUser(creatorId, "작성자", "profiles/creator", "ACTIVE");
        insertUser(recipientId, "떠난 사람", null, "DELETED");
        insertUser(3L, "멤버", null, "ACTIVE");
        Buzz buzz = saveBuzz(creatorId, NOW.minusHours(1), recipientId, 3L);
        saveThread(buzz, recipientId, NOW.minusMinutes(30));
        saveThread(buzz, 3L, NOW.minusMinutes(10));

        // when
        BuzzDetailInfo detail = buzzQueryRepositoryAdapter.findDetail(buzz.getId(), NOW).orElseThrow();

        // then
        assertThat(detail.creatorProfile()).isEqualTo(new BuzzUserProfileInfo("작성자", "profiles/creator"));
        assertThat(detail.threads()).extracting(BuzzThreadInfo::starterProfile)
                .containsExactly(null, new BuzzUserProfileInfo("멤버", null));
    }

    @Test
    @DisplayName("공개 대상은 비활성 사용자도 빠뜨리지 않고 활성 사용자에게만 프로필을 붙인다")
    void findAudience_ProfilesOnlyForActiveUsers() {
        // given
        insertUser(recipientId, "수신자", "profiles/recipient", "ACTIVE");
        insertUser(3L, "휴면", null, "DORMANT");
        Buzz buzz = saveBuzz(creatorId, NOW, recipientId, 3L);

        // when & then
        assertThat(buzzQueryRepositoryAdapter.findAudience(buzz.getId())).containsExactly(
                new BuzzAudienceInfo(recipientId, new BuzzUserProfileInfo("수신자", "profiles/recipient"), false),
                new BuzzAudienceInfo(3L, null, false));
    }

    @Test
    @DisplayName("받은 목록은 게시자가 비활성이어도 버즈를 주고 프로필만 비운다")
    void findReceived_InactiveCreator_KeepsBuzzWithoutProfile() {
        // given
        insertUser(creatorId, "작성자", null, "ACTIVE");
        insertUser(otherCreatorId, "떠난 작성자", null, "DELETED");
        Buzz active = saveBuzz(creatorId, NOW.minusHours(2), recipientId);
        Buzz inactive = saveBuzz(otherCreatorId, NOW.minusHours(1), recipientId);

        // when
        Slice<BuzzSummaryInfo> result = buzzQueryRepositoryAdapter.findReceived(
                recipientId, Set.of(), NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(BuzzSummaryInfo::buzzId)
                .containsExactly(inactive.getId(), active.getId());
        assertThat(result.getContent()).extracting(BuzzSummaryInfo::creatorProfile)
                .containsExactly(null, new BuzzUserProfileInfo("작성자", null));
    }

    @Test
    @DisplayName("받은 목록은 페이지 크기보다 많으면 다음 페이지가 있다고 알리고 페이지 크기만큼만 준다")
    void findReceived_SlicesByPageSize() {
        // given
        Buzz first = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        Buzz second = saveBuzz(creatorId, NOW.minusHours(2), recipientId);
        Buzz third = saveBuzz(creatorId, NOW.minusHours(3), recipientId);

        // when
        Slice<BuzzSummaryInfo> firstPage = buzzQueryRepositoryAdapter.findReceived(
                recipientId, Set.of(), NOW, PageRequest.of(0, 2));
        Slice<BuzzSummaryInfo> secondPage = buzzQueryRepositoryAdapter.findReceived(
                recipientId, Set.of(), NOW, PageRequest.of(1, 2));

        // then
        assertThat(firstPage.getContent()).extracting(BuzzSummaryInfo::buzzId)
                .containsExactly(first.getId(), second.getId());
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent()).extracting(BuzzSummaryInfo::buzzId).containsExactly(third.getId());
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("스레드 상세는 버즈의 게시자와 그 버즈에 속한 스레드를 활성 사용자 프로필과 함께 준다")
    void findThreadDetail_WithProfiles() {
        // given
        insertUser(creatorId, "작성자", null, "ACTIVE");
        insertUser(recipientId, "떠난 시작자", null, "DELETED");
        Buzz buzz = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        BuzzThread thread = saveThread(buzz, recipientId, NOW.minusMinutes(10));

        // when
        BuzzThreadDetailInfo detail = buzzQueryRepositoryAdapter.findThreadDetail(buzz.getId(), thread.getId(), NOW)
                .orElseThrow();

        // then
        assertThat(detail.creatorProfile()).isEqualTo(new BuzzUserProfileInfo("작성자", null));
        assertThat(detail.thread().threadId()).isEqualTo(thread.getId());
        assertThat(detail.thread().starterId()).isEqualTo(recipientId);
        assertThat(detail.thread().starterProfile()).isNull();
        assertThat(detail.thread().text()).isEqualTo("첫 글");
    }

    @Test
    @DisplayName("다른 버즈의 스레드 ID로 조회하면 버즈만 주고 스레드는 비운다")
    void findThreadDetail_OtherBuzzThread_EmptyThread() {
        // given
        Buzz buzz = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        Buzz other = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        BuzzThread otherThread = saveThread(other, recipientId, NOW.minusMinutes(10));

        // when
        BuzzThreadDetailInfo detail = buzzQueryRepositoryAdapter
                .findThreadDetail(buzz.getId(), otherThread.getId(), NOW).orElseThrow();

        // then
        assertThat(detail.buzzId()).isEqualTo(buzz.getId());
        assertThat(detail.thread()).isNull();
    }

    @Test
    @DisplayName("만료되었거나 삭제 표시된 버즈는 스레드 상세에서 빠진다")
    void findThreadDetail_ExcludesExpiredAndDeleted() {
        // given
        Buzz expired = saveBuzz(creatorId, NOW.minusHours(6), recipientId);
        BuzzThread expiredThread = saveThread(expired, recipientId, NOW.minusHours(5));
        Buzz deleted = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        BuzzThread deletedThread = saveThread(deleted, recipientId, NOW.minusMinutes(10));
        markDeleted(deleted.getId());

        // when & then
        assertThat(buzzQueryRepositoryAdapter.findThreadDetail(expired.getId(), expiredThread.getId(), NOW)).isEmpty();
        assertThat(buzzQueryRepositoryAdapter.findThreadDetail(deleted.getId(), deletedThread.getId(), NOW)).isEmpty();
    }

    @Test
    @DisplayName("답글은 그 스레드의 것만 쓴 순서대로 작성자 프로필과 함께 준다")
    void findComments_InOrderWithProfiles() {
        // given
        insertUser(3L, "답글러", "profiles/replier", "ACTIVE");
        Buzz buzz = saveBuzz(creatorId, NOW.minusHours(1), recipientId, 3L);
        BuzzThread thread = saveThread(buzz, recipientId, NOW.minusMinutes(30));
        BuzzThread otherThread = saveThread(buzz, 3L, NOW.minusMinutes(20));
        BuzzComment first = saveComment(buzz, thread, 3L, "첫 답글");
        BuzzComment second = saveComment(buzz, thread, recipientId, "둘째 답글");
        saveComment(buzz, otherThread, 3L, "다른 스레드 답글");

        // when
        List<BuzzCommentInfo> comments = buzzQueryRepositoryAdapter.findComments(thread.getId());

        // then
        assertThat(comments).extracting(BuzzCommentInfo::commentId).containsExactly(first.getId(), second.getId());
        assertThat(comments).extracting(BuzzCommentInfo::authorProfile)
                .containsExactly(new BuzzUserProfileInfo("답글러", "profiles/replier"), null);
        assertThat(comments.get(0).imageKeys()).containsExactly("buzz/c");
    }

    @Test
    @DisplayName("만료 시각이 기준 시각과 같은 버즈는 받은 목록에서 빠진다")
    void findReceived_ExcludesExpiresAtEqualToNow() {
        // given
        saveBuzz(creatorId, NOW.minusHours(6), recipientId);

        // when
        Slice<BuzzSummaryInfo> result = buzzQueryRepositoryAdapter.findReceived(
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
        Slice<BuzzSummaryInfo> result = buzzQueryRepositoryAdapter.findSent(creatorId, NOW, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(BuzzSummaryInfo::buzzId).containsExactly(live.getId());
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
        assertThat(buzzQueryRepositoryAdapter.findAudience(target.getId())).isEmpty();
        assertThat(buzzQueryRepositoryAdapter.findAudience(other.getId()))
                .extracting(BuzzAudienceInfo::userId).containsExactly(recipientId);
    }

    @Test
    @DisplayName("삭제 표시된 버즈는 단건·잠금 조회와 상세·받은 목록·보낸 목록·미확인 작성자에서 빠진다")
    void deletedBuzz_ExcludedFromAllReads() {
        // given
        Buzz deleted = saveBuzz(creatorId, NOW.minusHours(1), recipientId);
        markDeleted(deleted.getId());

        // when & then
        assertThat(buzzRepositoryAdapter.findById(deleted.getId())).isEmpty();
        assertThat(buzzRepositoryAdapter.findByIdForShare(deleted.getId())).isEmpty();
        assertThat(buzzRepositoryAdapter.findByIdForUpdate(deleted.getId())).isEmpty();
        assertThat(buzzQueryRepositoryAdapter.findDetail(deleted.getId(), NOW)).isEmpty();
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
        buzzRepositoryAdapter.save(loaded);
        entityManager.flush();
        entityManager.clear();

        // then
        Buzz reloaded = buzzRepositoryAdapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getText()).isEqualTo("고친 본문");
        assertThat(reloaded.getImageKeys()).containsExactly("buzz/b");
        assertThat(reloaded.getEditedAt()).isEqualTo(NOW.plusMinutes(10));
        assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
    }

    private BuzzThread saveThread(Buzz buzz, Long starterId, LocalDateTime createdAt) {
        BuzzThread thread = BuzzThread.create(
                buzz, starterId, true, ThreadVisibility.PUBLIC, "첫 글", List.of(), createdAt);
        entityManager.persist(thread);
        entityManager.flush();
        entityManager.clear();
        return thread;
    }

    private BuzzComment saveComment(Buzz buzz, BuzzThread thread, Long authorId, String text) {
        BuzzComment comment = BuzzComment.write(buzz, thread, authorId, text, List.of("buzz/c"), NOW);
        entityManager.persist(comment);
        entityManager.flush();
        entityManager.clear();
        return comment;
    }

    private void insertUser(Long userId, String nickname, String profileImage, String status) {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, email, nickname, profile_image, role, status) VALUES (?, ?, ?, ?, 'USER', ?)",
                userId, "user" + userId + "@buzz.test", nickname, profileImage, status);
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
        BuzzAudienceMember member = buzzAudienceMemberRepositoryAdapter.findByBuzzIdAndUserId(buzzId, userId)
                .orElseThrow();
        member.read();
        buzzAudienceMemberRepositoryAdapter.save(member);
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
