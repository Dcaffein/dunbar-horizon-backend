package com.example.DunbarHorizon.account.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserJpaRepository;
import com.example.DunbarHorizon.account.domain.User;
import com.example.DunbarHorizon.account.domain.UserStatus;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class UserJpaRepositoryTest {

    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private TestEntityManager em;

    @Test
    @DisplayName("ID와 활성화 상태가 모두 일치하는 유저를 조회한다")
    void findByIdAndStatus_Success() {
        // given
        User activeUser = User.builder()
                .email("active@test.com")
                .nickname("active")
                .status(UserStatus.ACTIVE)
                .build();
        User dormantUser = User.builder()
                .email("dormant@test.com")
                .nickname("dormant")
                .status(UserStatus.DORMANT)
                .build();

        em.persist(activeUser);
        em.persist(dormantUser);
        em.flush();

        // when
        Optional<User> result = userJpaRepository.findByIdAndStatus(activeUser.getId(), UserStatus.ACTIVE);
        Optional<User> resultFalse = userJpaRepository.findByIdAndStatus(dormantUser.getId(), UserStatus.ACTIVE);

        // then
        assertThat(result).isPresent();
        assertThat(resultFalse).isEmpty();
    }

    @Test
    @DisplayName("ID 목록으로 활성 사용자만 찾고 비활성화된 사용자는 다음 조회에서 제외한다")
    void findAllByIdInAndStatus_ExcludesInactiveUsers_AfterStatusChange() {
        // given
        User active = em.persist(User.createActive("active-batch@test.com", "active"));
        User deactivating = em.persist(User.createActive("deactivating-batch@test.com", "deactivating"));
        User dormant = em.persist(User.builder()
                .email("dormant-batch@test.com")
                .nickname("dormant")
                .status(UserStatus.DORMANT)
                .build());
        List<Long> ids = List.of(active.getId(), deactivating.getId(), dormant.getId());
        em.flush();
        em.clear();

        assertThat(userJpaRepository.findAllByIdInAndStatus(ids, UserStatus.ACTIVE))
                .extracting(User::getId)
                .containsExactlyInAnyOrder(active.getId(), deactivating.getId());

        // when
        em.find(User.class, deactivating.getId()).deactivate();
        em.flush();
        em.clear();
        List<User> profiles = userJpaRepository.findAllByIdInAndStatus(ids, UserStatus.ACTIVE);

        // then
        assertThat(profiles).extracting(User::getId).containsExactly(active.getId());
        assertThat(userJpaRepository.findByIdAndStatus(deactivating.getId(), UserStatus.ACTIVE)).isEmpty();
    }
}
