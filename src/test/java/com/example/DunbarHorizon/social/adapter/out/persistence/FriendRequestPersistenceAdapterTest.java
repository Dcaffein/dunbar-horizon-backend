package com.example.DunbarHorizon.social.adapter.out.persistence;

import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.social.domain.friend.FriendRequest;
import com.example.DunbarHorizon.social.domain.friend.FriendRequestStatus;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

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
        assertThat(friendRequestPersistenceAdapter.findAllByRequesterId(high.getId())).hasSize(1);
        assertThat(friendRequestPersistenceAdapter.findAllByReceiverIdAndStatus(low.getId(), FriendRequestStatus.PENDING)).hasSize(1);
        assertThat(friendRequestPersistenceAdapter.findAllByReceiverIdAndStatus(high.getId(), FriendRequestStatus.PENDING)).isEmpty();
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
        assertThat(friendRequestPersistenceAdapter.findAllByRequesterId(low.getId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM friend_requests WHERE requester_id = ?",
                Integer.class, low.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("읽은 뒤 요청이 지워졌으면 삭제와 상태 변경은 낙관적 잠금 실패로 끝난다")
    void deleteAndUpdate_FailOptimistically_DeletedAfterLoad() {
        // given
        friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(low.getId(), high.getId()));
        testEntityManager.clear();
        FriendRequest toDelete = friendRequestPersistenceAdapter.findBetween(low.getId(), high.getId()).orElseThrow();
        jdbcTemplate.update("DELETE FROM friend_requests WHERE requester_id = ?", low.getId());

        // when & then
        friendRequestPersistenceAdapter.delete(toDelete);
        assertThatThrownBy(testEntityManager::flush).isInstanceOf(OptimisticLockException.class);
    }

    @Test
    @DisplayName("상태 변경은 더티 체킹으로 저장되고, 읽은 뒤 지워졌으면 낙관적 잠금 실패로 끝난다")
    void updateStatus_PersistsOrFailsOptimistically() {
        // given
        friendRequestPersistenceAdapter.create(FriendTestFactory.createRequest(low.getId(), high.getId()));
        testEntityManager.clear();
        FriendRequest loaded = friendRequestPersistenceAdapter.findBetween(low.getId(), high.getId()).orElseThrow();

        // when
        loaded.updateStatus(high.getId(), FriendRequestStatus.HIDDEN);
        testEntityManager.flush();
        testEntityManager.clear();

        // then
        assertThat(friendRequestPersistenceAdapter.findAllByReceiverIdAndStatus(high.getId(), FriendRequestStatus.HIDDEN)).hasSize(1);
        FriendRequest hidden = friendRequestPersistenceAdapter.findBetween(low.getId(), high.getId()).orElseThrow();
        jdbcTemplate.update("DELETE FROM friend_requests WHERE requester_id = ?", low.getId());
        hidden.updateStatus(high.getId(), FriendRequestStatus.PENDING);
        assertThatThrownBy(testEntityManager::flush).isInstanceOf(OptimisticLockException.class);
    }

    private User createUser(String nickname) {
        return testEntityManager.persistAndFlush(User.builder()
                .email(nickname + "@jpa-requests.test")
                .nickname(nickname)
                .profileImage("profiles/" + nickname)
                .build());
    }
}
