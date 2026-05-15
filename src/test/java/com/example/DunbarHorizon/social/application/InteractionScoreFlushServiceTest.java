package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.port.out.InteractionScoreDeltaPort;
import com.example.DunbarHorizon.social.application.service.InteractionScoreFlushService;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionScoreFlushServiceTest {

    @InjectMocks
    private InteractionScoreFlushService flushService;

    @Mock
    private InteractionScoreDeltaPort deltaPort;

    @Mock
    private FriendshipRepository friendshipRepository;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;

    @Test
    @DisplayName("delta가 없으면 Neo4j 조회 없이 종료한다")
    void flush_emptyBuffer_doesNothing() {
        given(deltaPort.drainAll()).willReturn(Map.of());

        flushService.flush();

        verify(friendshipRepository, never()).findAllByIds(any());
        verify(friendshipRepository, never()).batchUpdateInterestScores(any(), any());
    }

    @Test
    @DisplayName("delta 누적값을 Friendship에 적용하고 batchUpdate를 호출한다")
    @SuppressWarnings("unchecked")
    void flush_appliesDeltaAndBatchUpdates() {
        String friendshipId = Friendship.generateCompositeId(USER_A, USER_B);
        Map<String, Map<String, Double>> deltas = Map.of(
                friendshipId, Map.of(
                        String.valueOf(USER_A), 10.0,
                        String.valueOf(USER_B), 10.0
                )
        );
        given(deltaPort.drainAll()).willReturn(deltas);

        Friendship friendship = createFriendship(friendshipId);
        given(friendshipRepository.findAllByIds(List.of(friendshipId))).willReturn(List.of(friendship));

        flushService.flush();

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(friendshipRepository).batchUpdateInterestScores(captor.capture(), any());

        List<Map<String, Object>> updates = captor.getValue();
        assertThat(updates).hasSize(2);
        assertThat(updates).allMatch(u -> u.containsKey("friendshipId") && u.containsKey("userId")
                && u.containsKey("interestScore") && u.containsKey("intimacy"));
    }

    private Friendship createFriendship(String id) {
        Friendship friendship = mock(Friendship.class);
        given(friendship.getId()).willReturn(id);
        given(friendship.getMyInterestScore(USER_A)).willReturn(10.0);
        given(friendship.getMyInterestScore(USER_B)).willReturn(10.0);
        given(friendship.getIntimacy()).willReturn(0.1);
        return friendship;
    }
}
