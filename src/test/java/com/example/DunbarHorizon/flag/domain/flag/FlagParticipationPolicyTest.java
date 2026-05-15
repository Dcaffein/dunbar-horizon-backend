package com.example.DunbarHorizon.flag.domain.flag;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagParticipationDuplicateException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagParticipationPolicyTest {

    @InjectMocks private FlagParticipationPolicy flagParticipationPolicy;

    @Mock private FlagParticipantRepository flagParticipantRepository;
    @Mock private FlagRepository flagRepository;
    @Mock private FriendshipChecker friendshipChecker;

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Flag recruitingFlag() {
        FlagSchedule schedule = FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
        Flag flag = Flag.create(HOST_ID, "테스트 플래그", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    @Test
    @DisplayName("친구이고 중복 참여가 아니면 FlagParticipant를 반환한다")
    void participate_Success() {
        // given
        Flag flag = recruitingFlag();
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(friendshipChecker.areFriends(HOST_ID, USER_ID)).willReturn(true);
        given(flagRepository.findByIdExclusive(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagParticipantRepository.isParticipating(FLAG_ID, USER_ID)).willReturn(false);
        given(flagParticipantRepository.countByFlagId(FLAG_ID)).willReturn(0);

        // when
        FlagParticipant result = flagParticipationPolicy.participate(FLAG_ID, USER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getParticipantId()).isEqualTo(USER_ID);
        verify(flagRepository).findById(FLAG_ID);
        verify(flagRepository).findByIdExclusive(FLAG_ID);
    }

    @Test
    @DisplayName("존재하지 않는 플래그에 참여하면 FlagNotFoundException이 발생한다")
    void participate_FlagNotFound_ThrowsException() {
        // given
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagParticipationPolicy.participate(FLAG_ID, USER_ID))
                .isInstanceOf(FlagNotFoundException.class);
    }

    @Test
    @DisplayName("호스트와 친구가 아니면 FlagAuthorizationException이 발생한다")
    void participate_NotFriend_ThrowsException() {
        // given
        Flag flag = recruitingFlag();
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(friendshipChecker.areFriends(HOST_ID, USER_ID)).willReturn(false);

        // when / then
        assertThatThrownBy(() -> flagParticipationPolicy.participate(FLAG_ID, USER_ID))
                .isInstanceOf(FlagAuthorizationException.class);
    }

    @Test
    @DisplayName("이미 참여 중이면 FlagParticipationDuplicateException이 발생한다")
    void participate_AlreadyParticipating_ThrowsException() {
        // given
        Flag flag = recruitingFlag();
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(friendshipChecker.areFriends(HOST_ID, USER_ID)).willReturn(true);
        given(flagRepository.findByIdExclusive(FLAG_ID)).willReturn(Optional.of(flag));
        given(flagParticipantRepository.isParticipating(FLAG_ID, USER_ID)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> flagParticipationPolicy.participate(FLAG_ID, USER_ID))
                .isInstanceOf(FlagParticipationDuplicateException.class);
    }
}
