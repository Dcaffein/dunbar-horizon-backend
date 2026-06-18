package com.example.DunbarHorizon.flag.adapter.out.persistence;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagInvitationJpaRepository;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitationStatus;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

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

    private FlagInvitation saveWithStatus(Long flagId, Long inviterId, Long inviteeId, FlagInvitationStatus status) {
        FlagInvitation inv = FlagInvitation.create(flagId, inviterId, inviteeId, LocalDateTime.now().plusHours(24));
        ReflectionTestUtils.setField(inv, "status", status);
        em.persist(inv);
        return inv;
    }

    @Test
    @DisplayName("inviteeId로 PENDING 초대만 createdAt 내림차순으로 조회한다")
    void findAllByInviteeIdAndStatus_ReturnsPendingOnly() {
        // given
        save(FLAG_ID, INVITER_ID, INVITEE_ID);                                             // PENDING
        save(2L, INVITER_ID, INVITEE_ID);                                                  // PENDING
        saveWithStatus(3L, INVITER_ID, INVITEE_ID, FlagInvitationStatus.ACCEPTED);        // 제외 대상
        saveWithStatus(4L, INVITER_ID, INVITEE_ID, FlagInvitationStatus.REJECTED);        // 제외 대상
        save(5L, INVITER_ID, OTHER_USER_ID);                                               // 다른 invitee — 제외 대상
        em.flush();
        em.clear();

        // when
        List<FlagInvitation> results = repository.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(INVITEE_ID, FlagInvitationStatus.PENDING);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(FlagInvitation::getInviteeId).containsOnly(INVITEE_ID);
        assertThat(results).extracting(FlagInvitation::getStatus).containsOnly(FlagInvitationStatus.PENDING);
    }

    @Test
    @DisplayName("inviterId로 PENDING 초대만 createdAt 내림차순으로 조회한다")
    void findAllByInviterIdAndStatus_ReturnsPendingOnly() {
        // given
        save(FLAG_ID, INVITER_ID, INVITEE_ID);                                             // PENDING
        save(2L, INVITER_ID, OTHER_USER_ID);                                               // PENDING
        saveWithStatus(3L, INVITER_ID, INVITEE_ID, FlagInvitationStatus.ACCEPTED);        // 제외 대상
        save(4L, OTHER_USER_ID, INVITEE_ID);                                               // 다른 inviter — 제외 대상
        em.flush();
        em.clear();

        // when
        List<FlagInvitation> results = repository.findAllByInviterIdAndStatusOrderByCreatedAtDesc(INVITER_ID, FlagInvitationStatus.PENDING);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(FlagInvitation::getInviterId).containsOnly(INVITER_ID);
        assertThat(results).extracting(FlagInvitation::getStatus).containsOnly(FlagInvitationStatus.PENDING);
    }
}
