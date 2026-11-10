package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.event.FlagDeletedEvent;
import com.example.DunbarHorizon.flag.domain.flag.event.FlagExpiryExemptionChangedEvent;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagDeadlinePassedException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagFullCapacityException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidBasicInfoException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class FlagTest {

    private static final long HOST_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private FlagSchedule recruitingSchedule() {
        return FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
    }

    private FlagSchedule endedSchedule() {
        return FlagSchedule.of(NOW.minusHours(3), NOW.minusHours(2), NOW.minusHours(1));
    }

    private Flag recruitingFlag() {
        Flag flag = Flag.create(HOST_ID, "테스트 플래그", "설명", 10, recruitingSchedule());
        ReflectionTestUtils.setField(flag, "id", 1L);
        return flag;
    }

    private Flag endedFlag() {
        Flag flag = Flag.create(HOST_ID, "종료된 플래그", "설명", 10, endedSchedule());
        ReflectionTestUtils.setField(flag, "id", 2L);
        return flag;
    }

    @Test
    @DisplayName("Flag를 직접 호출해도 비호스트는 앵콜을 생성할 수 없다")
    void createEncore_Fail_NonHost() {
        // given
        Flag parent = endedFlag();
        // when & then
        assertThatThrownBy(() -> parent.createEncore(99L, NOW.plusHours(1),
                NOW.plusHours(2), NOW.plusHours(3), false))
                .isInstanceOf(FlagAuthorizationException.class);
        assertThat(parent.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("기존 앵콜이 있다는 사실을 전달하면 Flag가 생성을 거절한다")
    void createEncore_Fail_ExistingEncore() {
        // given
        Flag parent = endedFlag();
        // when & then
        assertThatThrownBy(() -> parent.createEncore(HOST_ID, NOW.plusHours(1),
                NOW.plusHours(2), NOW.plusHours(3), true))
                .isInstanceOfSatisfying(FlagInvalidStatusException.class,
                        error -> assertThat(error.getCode()).isEqualTo("FLAG_ENCORE_ALREADY_EXISTS"));
        assertThat(parent.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("유효한 입력으로 플래그를 생성할 수 있다")
    void create_Success() {
        // when
        Flag flag = Flag.create(HOST_ID, "제목", "설명", 10, recruitingSchedule());

        // then
        assertThat(flag.getHostId()).isEqualTo(HOST_ID);
        assertThat(flag.getTitle()).isEqualTo("제목");
        assertThat(flag.getCapacity()).isEqualTo(10);
        assertThat(flag.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("hostId가 null이면 플래그 생성 시 예외가 발생한다")
    void create_NullHostId_ThrowsException() {
        assertThatThrownBy(() -> Flag.create(null, "제목", "설명", 10, recruitingSchedule()))
                .isInstanceOf(FlagInvalidStatusException.class);
    }

    @Test
    @DisplayName("title이 비어있으면 플래그 생성 시 FlagInvalidBasicInfoException이 발생한다")
    void create_BlankTitle_ThrowsException() {
        assertThatThrownBy(() -> Flag.create(HOST_ID, "  ", "설명", 10, recruitingSchedule()))
                .isInstanceOf(FlagInvalidBasicInfoException.class);
    }

    @Test
    @DisplayName("title이 20자를 초과하면 플래그 생성 시 FlagInvalidBasicInfoException이 발생한다")
    void create_TooLongTitle_ThrowsException() {
        String tooLong = "가".repeat(Flag.TITLE_MAX_LENGTH + 1);

        assertThatThrownBy(() -> Flag.create(HOST_ID, tooLong, "설명", 10, recruitingSchedule()))
                .isInstanceOf(FlagInvalidBasicInfoException.class);
    }

    @Test
    @DisplayName("title이 정확히 20자이면 플래그를 생성할 수 있다")
    void create_MaxLengthTitle_Success() {
        String boundary = "가".repeat(Flag.TITLE_MAX_LENGTH);

        assertThatNoException().isThrownBy(() ->
                Flag.create(HOST_ID, boundary, "설명", 10, recruitingSchedule()));
    }

    @Test
    @DisplayName("description이 500자를 초과하면 플래그 생성 시 FlagInvalidBasicInfoException이 발생한다")
    void create_TooLongDescription_ThrowsException() {
        String tooLong = "가".repeat(Flag.DESCRIPTION_MAX_LENGTH + 1);

        assertThatThrownBy(() -> Flag.create(HOST_ID, "제목", tooLong, 10, recruitingSchedule()))
                .isInstanceOf(FlagInvalidBasicInfoException.class);
    }

    @Test
    @DisplayName("모집 중인 플래그에 정상 참여할 수 있다")
    void participate_Success() {
        // given
        Flag flag = recruitingFlag();

        // when
        FlagParticipant participant = flag.participate(2L, 0);

        // then
        assertThat(participant.getParticipantId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("호스트는 자신의 플래그에 참여자로 등록될 수 없다")
    void participate_HostCannotJoin_ThrowsException() {
        // given
        Flag flag = recruitingFlag();

        // when / then
        assertThatThrownBy(() -> flag.participate(HOST_ID, 0))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("모집이 마감된 플래그에 참여하면 FlagDeadlinePassedException이 발생한다")
    void participate_DeadlinePassed_ThrowsException() {
        // given: WAITING status — deadline < now < startDateTime
        FlagSchedule waitingSchedule = FlagSchedule.of(NOW.minusMinutes(1), NOW.plusHours(1), NOW.plusHours(2));
        Flag flag = Flag.create(HOST_ID, "제목", "설명", 10, waitingSchedule);

        // when / then
        assertThatThrownBy(() -> flag.participate(2L, 0))
                .isInstanceOf(FlagDeadlinePassedException.class);
    }

    @Test
    @DisplayName("정원이 가득 찬 플래그에 참여하면 FlagFullCapacityException이 발생한다")
    void participate_FullCapacity_ThrowsException() {
        // given
        Flag flag = Flag.create(HOST_ID, "제목", "설명", 5, recruitingSchedule());

        // when / then
        assertThatThrownBy(() -> flag.participate(2L, 5))
                .isInstanceOf(FlagFullCapacityException.class);
    }

    @Test
    @DisplayName("capacity가 null이면 정원 제한 없이 참여 가능하다")
    void participate_NullCapacity_NoLimit() {
        // given
        Flag flag = Flag.create(HOST_ID, "제목", "설명", null, recruitingSchedule());
        ReflectionTestUtils.setField(flag, "id", 3L);

        // when / then
        assertThatNoException().isThrownBy(() -> flag.participate(2L, 999));
    }

    @Test
    @DisplayName("종료된 플래그에서 앵코르 플래그를 생성할 수 있다")
    void createEncore_Success() {
        // given
        Flag parent = endedFlag();

        // when
        Flag encore = parent.createEncore(HOST_ID, NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4), false);

        // then
        assertThat(encore.getTitle()).isEqualTo(parent.getTitle());
        assertThat(encore.getCapacity()).isEqualTo(parent.getCapacity());
        assertThat(encore.getParentId()).isEqualTo(parent.getId());
    }

    @Test
    @DisplayName("아직 활성 중인 플래그에서 앵코르를 생성하면 예외가 발생한다")
    void createEncore_ActiveFlag_ThrowsException() {
        // given
        Flag activeFlag = recruitingFlag();

        // when / then
        assertThatThrownBy(() ->
                activeFlag.createEncore(HOST_ID, NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4), false))
                .isInstanceOf(FlagInvalidStatusException.class);
    }

    @Test
    @DisplayName("호스트가 플래그를 삭제하면 softDelete된다")
    void delete_ByHost_Success() {
        // given
        Flag flag = recruitingFlag();

        // when
        flag.delete(HOST_ID);

        // then
        assertThat(flag.isDeleted()).isTrue();
        assertThat(flag.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("플래그 삭제 시 FlagDeletedEvent에 hostId가 포함된다")
    @SuppressWarnings("unchecked")
    void delete_PublishesEventWithHostId() {
        // given
        Flag flag = recruitingFlag();

        // when
        flag.delete(HOST_ID);

        // then
        java.util.Collection<Object> events = (java.util.Collection<Object>)
                ReflectionTestUtils.invokeMethod(flag, "domainEvents");
        FlagDeletedEvent event = (FlagDeletedEvent) events.stream()
                .filter(e -> e instanceof FlagDeletedEvent)
                .findFirst().orElseThrow();
        assertThat(event.hostId()).isEqualTo(HOST_ID);
        assertThat(event.flagId()).isEqualTo(flag.getId());
    }

    @Test
    @DisplayName("보존된 플래그를 삭제하면 면제가 꺼지고 해제 이벤트가 등록된다")
    @SuppressWarnings("unchecked")
    void delete_PreservedFlag_RegistersExemptionRelease() {
        // given
        Flag flag = endedFlag();
        ReflectionTestUtils.setField(flag, "autoExpiryExempt", true);

        // when
        flag.delete(HOST_ID);

        // then
        assertThat(flag.isAutoExpiryExempt()).isFalse();
        java.util.Collection<Object> events = (java.util.Collection<Object>)
                ReflectionTestUtils.invokeMethod(flag, "domainEvents");
        FlagExpiryExemptionChangedEvent released = (FlagExpiryExemptionChangedEvent) events.stream()
                .filter(e -> e instanceof FlagExpiryExemptionChangedEvent)
                .findFirst().orElseThrow();
        assertThat(released.exempt()).isFalse();
        assertThat(released.flagId()).isEqualTo(flag.getId());
        assertThat(released.hostId()).isEqualTo(HOST_ID);
    }

    @Test
    @DisplayName("보존 상태가 아닌 플래그를 삭제하면 해제 이벤트가 등록되지 않는다")
    @SuppressWarnings("unchecked")
    void delete_NotPreservedFlag_RegistersNoExemptionRelease() {
        // given
        Flag flag = endedFlag();

        // when
        flag.delete(HOST_ID);

        // then
        java.util.Collection<Object> events = (java.util.Collection<Object>)
                ReflectionTestUtils.invokeMethod(flag, "domainEvents");
        assertThat(events).noneMatch(e -> e instanceof FlagExpiryExemptionChangedEvent);
    }

    @Test
    @DisplayName("앙코르를 삭제하면 parentId를 비우고 이벤트에는 원래 parentId를 남긴다")
    @SuppressWarnings("unchecked")
    void delete_Encore_ClearsParentIdAndKeepsItInEvents() {
        // given
        Long parentId = 99L;
        Flag encore = endedFlag();
        ReflectionTestUtils.setField(encore, "parentId", parentId);
        ReflectionTestUtils.setField(encore, "autoExpiryExempt", true);

        // when
        encore.delete(HOST_ID);

        // then
        assertThat(encore.getParentId()).isNull();
        java.util.Collection<Object> events = (java.util.Collection<Object>)
                ReflectionTestUtils.invokeMethod(encore, "domainEvents");
        FlagDeletedEvent deleted = (FlagDeletedEvent) events.stream()
                .filter(e -> e instanceof FlagDeletedEvent)
                .findFirst().orElseThrow();
        FlagExpiryExemptionChangedEvent released = (FlagExpiryExemptionChangedEvent) events.stream()
                .filter(e -> e instanceof FlagExpiryExemptionChangedEvent)
                .findFirst().orElseThrow();
        assertThat(deleted.parentId()).isEqualTo(parentId);
        assertThat(released.parentId()).isEqualTo(parentId);
    }

    @Test
    @DisplayName("보존하면 보존 필드가 켜지고 삭제되지 않는다")
    void preserve_SetsPreserved() {
        // given
        Flag flag = endedFlag();

        // when
        flag.preserve();

        // then
        assertThat(flag.isAutoExpiryExempt()).isTrue();
        assertThat(flag.isDeleted()).isFalse();
        assertThat(flag.isConcluded()).isTrue();
    }

    @Test
    @DisplayName("만료하면 주어진 시각으로 소프트 삭제되고 보존 필드는 꺼진 채다")
    void expire_SoftDeletesAtGivenTime() {
        // given
        Flag flag = endedFlag();
        LocalDateTime now = LocalDateTime.now();

        // when
        flag.expire(now);

        // then
        assertThat(flag.getDeletedAt()).isEqualTo(now);
        assertThat(flag.isAutoExpiryExempt()).isFalse();
        assertThat(flag.isConcluded()).isTrue();
    }

    @Test
    @DisplayName("결말이 난 플래그에 다시 결말을 내면 거부된다")
    void conclude_Twice_Rejected() {
        // given
        Flag preserved = endedFlag();
        preserved.preserve();
        Flag expired = endedFlag();
        expired.expire(LocalDateTime.now());

        // when / then
        assertThatThrownBy(preserved::preserve).isInstanceOf(FlagInvalidStatusException.class);
        assertThatThrownBy(() -> preserved.expire(LocalDateTime.now())).isInstanceOf(FlagInvalidStatusException.class);
        assertThatThrownBy(expired::preserve).isInstanceOf(FlagInvalidStatusException.class);
        assertThatThrownBy(() -> expired.expire(LocalDateTime.now())).isInstanceOf(FlagInvalidStatusException.class);
    }

    @Test
    @DisplayName("결말 전에 호스트가 삭제한 플래그에도 결말을 낼 수 없다")
    void conclude_AfterHostDeletion_Rejected() {
        // given
        Flag flag = endedFlag();
        flag.delete(HOST_ID);

        // when / then
        assertThatThrownBy(flag::preserve).isInstanceOf(FlagInvalidStatusException.class);
    }

    @Test
    @DisplayName("호스트가 아닌 사용자가 플래그 삭제를 시도하면 FlagAuthorizationException이 발생한다")
    void delete_ByNonHost_ThrowsException() {
        // given
        Flag flag = recruitingFlag();

        // when / then
        assertThatThrownBy(() -> flag.delete(99L))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("모집 종료 호출 후 마감일이 현재 시각으로 업데이트된다")
    void closeRecruitment_Success() {
        // given
        Flag flag = recruitingFlag();
        LocalDateTime before = LocalDateTime.now();

        // when
        flag.closeRecruitment(HOST_ID);

        // then
        assertThat(flag.getSchedule().getDeadline()).isAfterOrEqualTo(before);
        assertThat(flag.isRecruiting()).isFalse();
    }

    @Test
    @DisplayName("모집 중이 아닌 상태에서 closeRecruitment를 호출하면 예외가 발생한다")
    void closeRecruitment_NotRecruiting_ThrowsException() {
        // given: WAITING status
        FlagSchedule waitingSchedule = FlagSchedule.of(NOW.minusMinutes(1), NOW.plusHours(1), NOW.plusHours(2));
        Flag flag = Flag.create(HOST_ID, "제목", "설명", 10, waitingSchedule);

        // when / then
        assertThatThrownBy(() -> flag.closeRecruitment(HOST_ID))
                .isInstanceOf(FlagInvalidStatusException.class);
    }
}
