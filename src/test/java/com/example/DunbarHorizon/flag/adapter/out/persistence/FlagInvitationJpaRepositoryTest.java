package com.example.DunbarHorizon.flag.adapter.out.persistence;

import com.example.DunbarHorizon.support.FlagInvitationFixture;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagInvitationJpaRepository;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.FlagStatus;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        FlagInvitation inv = FlagInvitationFixture.invitation(flagId, inviterId, inviteeId);
        em.persist(inv);
        return inv;
    }

    private Flag saveFlag(FlagSchedule schedule, boolean preserved) {
        Flag flag = Flag.create(INVITER_ID, "플래그 제목", "플래그 설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "preserved", preserved);
        em.persist(flag);
        return flag;
    }

    private static FlagSchedule inActivitySchedule() {
        LocalDateTime now = LocalDateTime.now();
        return FlagSchedule.of(now.minusHours(2), now.minusHours(1), now.plusHours(1));
    }

    private static FlagSchedule endedSchedule() {
        LocalDateTime now = LocalDateTime.now();
        return FlagSchedule.of(now.minusHours(5), now.minusHours(4), now.minusHours(3));
    }

    private static FlagSchedule waitingSchedule() {
        LocalDateTime now = LocalDateTime.now();
        return FlagSchedule.of(now.minusHours(1), now.plusHours(1), now.plusHours(2));
    }

    @Test
    @DisplayName("inviteeId로 초대를 createdAt 내림차순으로 조회한다")
    void findAllByInviteeId_FiltersByInvitee() {
        // given
        save(FLAG_ID, INVITER_ID, INVITEE_ID);
        save(2L, INVITER_ID, INVITEE_ID);
        save(3L, INVITER_ID, OTHER_USER_ID);        // 다른 invitee — 제외 대상
        em.flush();
        em.clear();

        // when
        List<FlagInvitation> results = repository.findAllByInviteeIdOrderByCreatedAtDesc(INVITEE_ID);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(FlagInvitation::getInviteeId).containsOnly(INVITEE_ID);
    }

    @Test
    @DisplayName("inviterId로 초대를 createdAt 내림차순으로 조회한다")
    void findAllByInviterId_FiltersByInviter() {
        // given
        save(FLAG_ID, INVITER_ID, INVITEE_ID);
        save(2L, INVITER_ID, OTHER_USER_ID);
        save(3L, OTHER_USER_ID, INVITEE_ID);        // 다른 inviter — 제외 대상
        em.flush();
        em.clear();

        // when
        List<FlagInvitation> results = repository.findAllByInviterIdOrderByCreatedAtDesc(INVITER_ID);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(FlagInvitation::getInviterId).containsOnly(INVITER_ID);
    }

    @Test
    @DisplayName("같은 Flag에 같은 invitee로 초대가 있으면 존재로 판정한다")
    void existsByFlagIdAndInviteeId_DetectsDuplicate() {
        // given
        save(FLAG_ID, INVITER_ID, INVITEE_ID);
        em.flush();
        em.clear();

        // when & then
        assertThat(repository.existsByFlagIdAndInviteeId(FLAG_ID, INVITEE_ID)).isTrue();
        assertThat(repository.existsByFlagIdAndInviteeId(FLAG_ID, OTHER_USER_ID)).isFalse();
        assertThat(repository.existsByFlagIdAndInviteeId(2L, INVITEE_ID)).isFalse();
    }

    @Test
    @DisplayName("초대가 삭제되면 중복 판정에서 빠져 재초대가 가능하다")
    void existsByFlagIdAndInviteeId_AfterDeletion_AllowsReinvite() {
        // given
        FlagInvitation invitation = save(FLAG_ID, INVITER_ID, INVITEE_ID);
        em.flush();
        Long invitationId = invitation.getId();

        // when
        repository.deleteById(invitationId);
        em.flush();
        em.clear();

        // then
        assertThat(repository.existsByFlagIdAndInviteeId(FLAG_ID, INVITEE_ID)).isFalse();
    }

    @Test
    @DisplayName("flagId로 초대받은 유저 ID 집합을 조회한다")
    void findInviteeIdsByFlagId_ReturnsInviteeIds() {
        // given
        save(FLAG_ID, INVITER_ID, INVITEE_ID);
        save(FLAG_ID, INVITER_ID, OTHER_USER_ID);
        save(2L, INVITER_ID, 40L);                  // 다른 flag — 제외 대상
        em.flush();
        em.clear();

        // when
        Set<Long> inviteeIds = repository.findInviteeIdsByFlagId(FLAG_ID);

        // then
        assertThat(inviteeIds).containsExactlyInAnyOrder(INVITEE_ID, OTHER_USER_ID);
    }

    @Test
    @DisplayName("활동 중인 플래그와 보존된 종료 플래그의 초대가 삭제된다")
    void hardDeleteByFlagStartDateTimeBefore_DeletesStartedFlagInvitations() {
        // given
        Flag inActivity = saveFlag(inActivitySchedule(), false);
        Flag preservedEnded = saveFlag(endedSchedule(), true);
        save(inActivity.getId(), INVITER_ID, INVITEE_ID);
        save(preservedEnded.getId(), INVITER_ID, INVITEE_ID);
        em.flush();
        em.clear();

        // when
        repository.hardDeleteByFlagStartDateTimeBefore(LocalDateTime.now());

        // then
        assertThat(repository.findAllByInviteeIdOrderByCreatedAtDesc(INVITEE_ID)).isEmpty();
    }

    @Test
    @DisplayName("마감이 지났어도 시작 전인 플래그의 초대는 남는다 — 일정 변경으로 모집이 다시 열릴 수 있다")
    void hardDeleteByFlagStartDateTimeBefore_KeepsWaitingFlagInvitations() {
        // given
        Flag waiting = saveFlag(waitingSchedule(), false);
        save(waiting.getId(), INVITER_ID, INVITEE_ID);
        em.flush();
        em.clear();

        // when
        repository.hardDeleteByFlagStartDateTimeBefore(LocalDateTime.now());

        // then
        assertThat(repository.findAllByInviteeIdOrderByCreatedAtDesc(INVITEE_ID))
                .extracting(FlagInvitation::getFlagId).containsExactly(waiting.getId());
    }

    @Test
    @DisplayName("소프트 삭제된 플래그의 초대는 이 쿼리로 지워지지 않는다 — 퍼지 소관이다")
    void hardDeleteByFlagStartDateTimeBefore_SkipsSoftDeletedFlags() {
        // given
        Flag softDeleted = saveFlag(endedSchedule(), false);
        softDeleted.softDelete();
        save(softDeleted.getId(), INVITER_ID, INVITEE_ID);
        em.flush();
        em.clear();

        // when
        repository.hardDeleteByFlagStartDateTimeBefore(LocalDateTime.now());

        // then
        assertThat(repository.findAllByInviteeIdOrderByCreatedAtDesc(INVITEE_ID)).hasSize(1);
    }

    @Test
    @DisplayName("삭제 대상은 calculateStatus가 IN_ACTIVITY 또는 ENDED로 판정하는 플래그와 같다")
    void hardDeleteByFlagStartDateTimeBefore_MatchesStartedStatusAtBoundary() {
        // given — 시작 시각이 기준 시각 직전, 정각, 직후인 플래그
        LocalDateTime asOf = LocalDateTime.now().withNano(0);
        List<Flag> flags = List.of(
                saveFlag(startingAt(asOf.minusSeconds(1)), false),
                saveFlag(startingAt(asOf), false),
                saveFlag(startingAt(asOf.plusSeconds(1)), false));
        flags.forEach(flag -> save(flag.getId(), INVITER_ID, INVITEE_ID));
        em.flush();
        em.clear();

        Set<Long> startedByDomain = flags.stream()
                .filter(flag -> {
                    FlagStatus status = flag.getSchedule().calculateStatus(asOf);
                    return status == FlagStatus.IN_ACTIVITY || status == FlagStatus.ENDED;
                })
                .map(Flag::getId)
                .collect(Collectors.toSet());

        // when
        repository.hardDeleteByFlagStartDateTimeBefore(asOf);

        // then
        Set<Long> remaining = repository.findAllByInviteeIdOrderByCreatedAtDesc(INVITEE_ID).stream()
                .map(FlagInvitation::getFlagId)
                .collect(Collectors.toSet());
        Set<Long> deletedByQuery = flags.stream()
                .map(Flag::getId)
                .filter(id -> !remaining.contains(id))
                .collect(Collectors.toSet());
        assertThat(deletedByQuery).isEqualTo(startedByDomain).containsExactly(flags.get(0).getId());
    }

    private static FlagSchedule startingAt(LocalDateTime start) {
        return FlagSchedule.of(start.minusHours(1), start, start.plusHours(1));
    }
}
