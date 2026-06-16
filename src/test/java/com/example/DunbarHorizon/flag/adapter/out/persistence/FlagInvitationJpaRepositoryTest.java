package com.example.DunbarHorizon.flag.adapter.out.persistence;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagInvitationJpaRepository;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class FlagInvitationJpaRepositoryTest {

    @Autowired private FlagInvitationJpaRepository repository;
    @Autowired private TestEntityManager em;

    private static final Long FLAG_ID = 1L;
    private static final Long INVITER_ID = 10L;
    private static final Long INVITEE_ID = 20L;
    private static final Long OTHER_USER_ID = 30L;

    private FlagInvitation save(Long flagId, Long inviterId, Long inviteeId) {
        FlagInvitation inv = FlagInvitation.create(flagId, inviterId, inviteeId, LocalDateTime.now().plusHours(24));
        em.persist(inv);
        return inv;
    }

    @Test
    @DisplayName("inviteeId로 받은 초대 목록을 createdAt 내림차순으로 조회한다")
    void findAllByInviteeIdOrderByCreatedAtDesc_ReturnsInOrder() {
        // given
        FlagInvitation first = save(FLAG_ID, INVITER_ID, INVITEE_ID);
        FlagInvitation second = save(2L, INVITER_ID, INVITEE_ID);
        save(3L, INVITER_ID, OTHER_USER_ID); // 다른 invitee — 제외 대상
        em.flush();
        em.clear();

        // when
        List<FlagInvitation> results = repository.findAllByInviteeIdOrderByCreatedAtDesc(INVITEE_ID);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(FlagInvitation::getInviteeId)
                .containsOnly(INVITEE_ID);
    }

    @Test
    @DisplayName("inviterId로 보낸 초대 목록을 createdAt 내림차순으로 조회한다")
    void findAllByInviterIdOrderByCreatedAtDesc_ReturnsInOrder() {
        // given
        save(FLAG_ID, INVITER_ID, INVITEE_ID);
        save(2L, INVITER_ID, OTHER_USER_ID);
        save(3L, OTHER_USER_ID, INVITEE_ID); // 다른 inviter — 제외 대상
        em.flush();
        em.clear();

        // when
        List<FlagInvitation> results = repository.findAllByInviterIdOrderByCreatedAtDesc(INVITER_ID);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(FlagInvitation::getInviterId)
                .containsOnly(INVITER_ID);
    }
}
