package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.friend.exception.FriendRequestConflictException;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaRepositoryTest
@Import(FriendRequestPersistenceAdapter.class)
class FriendRequestPersistenceAdapterTest {

    @Autowired private FriendRequestPersistenceAdapter friendRequestPersistenceAdapter;
    @Autowired private TestEntityManager testEntityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private User low;
    private User high;

    @BeforeEach
    void setUp() {
        low = createUser("low");
        high = createUser("high");
    }

    @Test
    @DisplayName("높은 ID가 보낸 요청을 두 사용자 어느 순서로도 찾고 방향과 상태를 복원한다")
    void findBetween_RestoresDirection_FromEitherOrder() {
        // given
        friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(high.getId(), low.getId()));
        testEntityManager.clear();

        // when & then
        for (var found : new FriendRequest[]{
                friendRequestPersistenceAdapter.findBetween(low.getId(), high.getId()).orElseThrow(),
                friendRequestPersistenceAdapter.findBetween(high.getId(), low.getId()).orElseThrow()}) {
            assertThat(found.getRequesterId()).isEqualTo(high.getId());
            assertThat(found.getReceiverId()).isEqualTo(low.getId());
            assertThat(found.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
        }
        assertThat(friendRequestPersistenceAdapter.existsBetween(low.getId(), high.getId())).isTrue();
    }

    @Test
    @DisplayName("친구 요청의 단건 조회와 존재 확인은 각각 한 SQL 문장으로 처리한다")
    void pairLookups_UseOneStatementEach_ForActiveUsers() {
        // given
        friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(high.getId(), low.getId()));
        testEntityManager.clear();
        var statistics = testEntityManager.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        boolean wasEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);

        try {
            // when
            long beforeFind = statistics.getPrepareStatementCount();
            var request = friendRequestPersistenceAdapter.findBetween(low.getId(), high.getId());
            long afterFind = statistics.getPrepareStatementCount();
            boolean exists = friendRequestPersistenceAdapter.existsBetween(low.getId(), high.getId());
            long afterExists = statistics.getPrepareStatementCount();

            // then
            assertThat(request).isPresent();
            assertThat(exists).isTrue();
            assertThat(List.of(afterFind - beforeFind, afterExists - afterFind)).containsExactly(1L, 1L);
        } finally {
            statistics.setStatisticsEnabled(wasEnabled);
        }
    }

    @Test
    @DisplayName("같은 쌍의 요청은 방향과 무관하게 한 건만 저장된다")
    void create_ThrowsDuplicate_SameOrOppositeDirection() {
        // given
        friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(low.getId(), high.getId()));
        testEntityManager.clear();

        // when & then
        assertThatThrownBy(() -> friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(low.getId(), high.getId())))
                .isInstanceOf(DuplicateFriendRequestException.class);
        testEntityManager.clear();
        assertThatThrownBy(() -> friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(high.getId(), low.getId())))
                .isInstanceOf(DuplicateFriendRequestException.class);
        testEntityManager.clear();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM friend_requests WHERE requester_id IN (?, ?)",
                Integer.class, low.getId(), high.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("상대가 비활성이면 요청 조회에서 없는 것으로 다루고 행은 남긴다")
    void lookups_TreatAsMissing_InactiveCounterpart() {
        // given
        friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(low.getId(), high.getId()));
        high.deactivate();
        testEntityManager.flush();
        testEntityManager.clear();

        // when & then
        assertThat(friendRequestPersistenceAdapter.findBetween(low.getId(), high.getId())).isEmpty();
        assertThat(friendRequestPersistenceAdapter.existsBetween(low.getId(), high.getId())).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM friend_requests WHERE requester_id = ?",
                Integer.class, low.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("수신자가 비활성이면 요청 저장은 사용자 없음으로 끝나고 행이 생기지 않는다")
    void create_Fails_InactiveReceiver() {
        // given
        high.deactivate();
        testEntityManager.flush();

        // when & then
        assertThatThrownBy(() -> friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(low.getId(), high.getId())))
                .isInstanceOf(UserReferenceNotFoundException.class)
                .hasMessageContaining("userId=" + high.getId() + "}");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM friend_requests WHERE requester_id = ?",
                Integer.class, low.getId())).isZero();
    }

    @Test
    @DisplayName("요청자와 수신자가 모두 비활성이면 요청자 ID로 사용자 없음 오류를 낸다")
    void create_ReportsRequester_WhenBothParticipantsAreInactive() {
        // given
        low.deactivate();
        high.deactivate();
        testEntityManager.flush();

        // when & then
        assertThatThrownBy(() -> friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(high.getId(), low.getId())))
                .isInstanceOf(UserReferenceNotFoundException.class)
                .hasMessageContaining("userId=" + high.getId() + "}");
    }

    @Test
    @DisplayName("읽은 뒤 요청이 지워졌으면 삭제는 영향 행 0건으로 요청 충돌이 된다")
    void delete_ThrowsConflict_DeletedAfterLoad() {
        // given
        friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(low.getId(), high.getId()));
        testEntityManager.clear();
        FriendRequest toDelete = friendRequestPersistenceAdapter.findBetween(low.getId(), high.getId()).orElseThrow();
        jdbcTemplate.update("DELETE FROM friend_requests WHERE requester_id = ?", low.getId());

        // when & then
        assertThatThrownBy(() -> friendRequestPersistenceAdapter.delete(toDelete))
                .isInstanceOf(FriendRequestConflictException.class);
    }

    @Test
    @DisplayName("상태 변경은 saveStatus로 저장되고, 읽은 뒤 지워졌으면 영향 행 0건으로 요청 충돌이 된다")
    void saveStatus_PersistsOrThrowsConflict() {
        // given
        friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(low.getId(), high.getId()));
        testEntityManager.clear();
        FriendRequest loaded = friendRequestPersistenceAdapter.findBetween(low.getId(), high.getId()).orElseThrow();

        // when
        loaded.updateStatus(high.getId(), FriendRequestStatus.HIDDEN);
        friendRequestPersistenceAdapter.saveStatus(loaded);
        testEntityManager.clear();

        // then
        FriendRequest hidden = friendRequestPersistenceAdapter.findBetween(low.getId(), high.getId()).orElseThrow();
        assertThat(hidden.getStatus()).isEqualTo(FriendRequestStatus.HIDDEN);
        jdbcTemplate.update("DELETE FROM friend_requests WHERE requester_id = ?", low.getId());
        hidden.updateStatus(high.getId(), FriendRequestStatus.PENDING);
        assertThatThrownBy(() -> friendRequestPersistenceAdapter.saveStatus(hidden))
                .isInstanceOf(FriendRequestConflictException.class);
    }

    private User createUser(String nickname) {
        return testEntityManager.persistAndFlush(User.builder()
                .email(nickname + "@jpa-requests.test")
                .nickname(nickname)
                .profileImage("profiles/" + nickname)
                .build());
    }
}
