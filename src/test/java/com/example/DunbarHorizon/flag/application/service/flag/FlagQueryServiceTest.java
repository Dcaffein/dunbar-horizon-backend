package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.application.dto.result.FlagDetailResult;
import com.example.DunbarHorizon.flag.application.dto.result.FlagResult;
import com.example.DunbarHorizon.flag.application.port.in.FlagRole;
import com.example.DunbarHorizon.flag.application.port.out.FlagUserPort;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagParticipant;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FlagQueryServiceTest {

    @InjectMocks private FlagQueryService flagQueryService;

    @Mock private FlagRepository flagRepository;
    @Mock private FlagUserPort flagUserPort;

    private static final Long USER_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Flag createRecruitingFlag(Long id) {
        FlagSchedule schedule = FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
        Flag flag = Flag.create(USER_ID, "테스트 플래그", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", id);
        return flag;
    }

    private FlagUserInfo userInfo(Long userId) {
        return new FlagUserInfo(userId, "사용자" + userId, null);
    }

    @Test
    @DisplayName("HOST 역할로 조회하면 호스팅 중인 플래그 목록이 반환된다")
    void getMyFlagsByRole_Host_ReturnsManagedFlags() {
        // given
        Flag flag = createRecruitingFlag(1L);
        given(flagRepository.findAllByHostId(USER_ID)).willReturn(List.of(flag));
        given(flagUserPort.findUserInfosByIds(Set.of(USER_ID))).willReturn(Map.of(USER_ID, userInfo(USER_ID)));

        // when
        List<FlagResult> result = flagQueryService.getMyFlagsByRole(USER_ID, FlagRole.HOST);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("테스트 플래그");
    }

    @Test
    @DisplayName("PARTICIPANT 역할로 조회하면 참여 중인 플래그 목록이 반환된다")
    void getMyFlagsByRole_Participant_ReturnsParticipatingFlags() {
        // given
        Flag flag = createRecruitingFlag(1L);
        given(flagRepository.findFlagIdsByParticipantId(USER_ID)).willReturn(List.of(1L));
        given(flagRepository.findAllByIdIn(List.of(1L))).willReturn(List.of(flag));
        given(flagUserPort.findUserInfosByIds(Set.of(USER_ID))).willReturn(Map.of(USER_ID, userInfo(USER_ID)));

        // when
        List<FlagResult> result = flagQueryService.getMyFlagsByRole(USER_ID, FlagRole.PARTICIPANT);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("참여 플래그가 없으면 빈 리스트가 반환된다")
    void getMyFlagsByRole_ParticipantNoFlags_ReturnsEmpty() {
        // given
        given(flagRepository.findFlagIdsByParticipantId(USER_ID)).willReturn(List.of());

        // when
        List<FlagResult> result = flagQueryService.getMyFlagsByRole(USER_ID, FlagRole.PARTICIPANT);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 플래그를 상세 조회하면 FlagNotFoundException이 발생한다")
    void getFlagDetail_NotFound_ThrowsException() {
        // given
        given(flagRepository.findById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagQueryService.getFlagDetail(999L, USER_ID))
                .isInstanceOf(FlagNotFoundException.class);
    }

    @Test
    @DisplayName("플래그 상세 조회 시 호스트는 HOST 역할로 응답받는다")
    void getFlagDetail_ViewerIsHost_ReturnsHostRole() {
        // given
        Flag flag = createRecruitingFlag(1L);
        given(flagRepository.findById(1L)).willReturn(Optional.of(flag));
        given(flagRepository.findAllParticipants(1L)).willReturn(List.of());
        given(flagUserPort.findUserInfosByIds(anySet()))
                .willReturn(Map.of(USER_ID, userInfo(USER_ID)));

        // when
        FlagDetailResult result = flagQueryService.getFlagDetail(1L, USER_ID); // viewerId == hostId

        // then
        assertThat(result.role()).isEqualTo(FlagRole.HOST);
    }

    @Test
    @DisplayName("플래그 상세 조회 시 참여자는 PARTICIPANT 역할로 응답받는다")
    void getFlagDetail_ViewerIsParticipant_ReturnsParticipantRole() {
        // given
        Long viewerId = 99L;
        Flag flag = createRecruitingFlag(1L);

        FlagParticipant mockParticipant = mock(FlagParticipant.class);
        given(mockParticipant.getParticipantId()).willReturn(viewerId);
        given(mockParticipant.getCreatedAt()).willReturn(NOW);

        given(flagRepository.findById(1L)).willReturn(Optional.of(flag));
        given(flagRepository.findAllParticipants(1L)).willReturn(List.of(mockParticipant));
        given(flagUserPort.findUserInfosByIds(anySet()))
                .willReturn(Map.of(USER_ID, userInfo(USER_ID), viewerId, userInfo(viewerId)));

        // when
        FlagDetailResult result = flagQueryService.getFlagDetail(1L, viewerId);

        // then
        assertThat(result.role()).isEqualTo(FlagRole.PARTICIPANT);
    }

    @Test
    @DisplayName("플래그 상세 조회 시 호스트도 참여자도 아닌 사용자는 FlagAuthorizationException이 발생한다")
    void getFlagDetail_ViewerHasNoRelation_ThrowsException() {
        Long viewerId = 999L;
        Flag flag = createRecruitingFlag(1L);

        given(flagRepository.findById(1L)).willReturn(Optional.of(flag));
        given(flagRepository.findAllParticipants(1L)).willReturn(List.of());

        assertThatThrownBy(() -> flagQueryService.getFlagDetail(1L, viewerId))
                .isInstanceOf(FlagAuthorizationException.class);
    }
}
