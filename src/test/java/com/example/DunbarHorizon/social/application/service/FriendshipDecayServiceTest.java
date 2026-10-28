package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.domain.friend.FriendshipDecayPolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendshipDecayServiceTest {

    @InjectMocks private FriendshipDecayService friendshipDecayService;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private FriendshipDecayPolicy decayPolicy;

    @Test
    @DisplayName("정책이 정한 감쇄율과 하한과 기준 시각을 그대로 저장소에 넘긴다")
    void processDecay_passesPolicyValuesToRepository() {
        // given
        LocalDateTime thresholdTime = LocalDateTime.of(2026, 10, 1, 3, 0);
        org.mockito.Mockito.when(decayPolicy.getDecayRate()).thenReturn(0.967);
        org.mockito.Mockito.when(decayPolicy.getMinThreshold()).thenReturn(1.0);
        org.mockito.Mockito.when(decayPolicy.getDecayThresholdTime()).thenReturn(thresholdTime);

        // when
        friendshipDecayService.processDecay();

        // then
        verify(friendshipRepository).applyDecay(0.967, 1.0, thresholdTime);
    }

    @Test
    @DisplayName("저장소 실패를 삼키지 않고 호출자에게 올린다")
    void processDecay_rethrowsRepositoryFailure() {
        // given
        doThrow(new TransientDataAccessResourceException("deadlock"))
                .when(friendshipRepository).applyDecay(anyDouble(), anyDouble(), any());

        // when & then
        assertThatThrownBy(() -> friendshipDecayService.processDecay())
                .isInstanceOf(TransientDataAccessResourceException.class);
    }
}
