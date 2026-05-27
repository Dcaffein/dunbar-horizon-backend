package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.port.out.FriendshipDelta;
import com.example.DunbarHorizon.social.application.port.out.InteractionScoreDeltaPort;
import com.example.DunbarHorizon.social.application.service.InteractionScoreFlushService;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @DisplayName("unilateral delta는 해당 userId에만 adjustInterestScore를 호출한다")
    @SuppressWarnings("unchecked")
    void flush_unilateral_appliesOneSide() {
        String friendshipId = Friendship.generateCompositeId(USER_A, USER_B);
        FriendshipDelta delta = new FriendshipDelta(Map.of(USER_A, 1.0), 0.0);
        given(deltaPort.drainAll()).willReturn(Map.of(friendshipId, delta));

        Friendship friendship = mockFriendship(friendshipId);
        given(friendshipRepository.findAllByIds(any())).willReturn(List.of(friendship));

        flushService.flush();

        verify(friendship).adjustInterestScore(USER_A, 1.0);
        verify(friendship, never()).adjustMutualInterestScore(anyDouble());

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(friendshipRepository).batchUpdateInterestScores(captor.capture(), any());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("mutual delta는 adjustMutualInterestScore를 호출하고 양쪽 userId로 update를 생성한다")
    @SuppressWarnings("unchecked")
    void flush_mutual_appliesBothSides() {
        String friendshipId = Friendship.generateCompositeId(USER_A, USER_B);
        FriendshipDelta delta = new FriendshipDelta(Map.of(), 10.0);
        given(deltaPort.drainAll()).willReturn(Map.of(friendshipId, delta));

        Friendship friendship = mockFriendship(friendshipId);
        given(friendshipRepository.findAllByIds(any())).willReturn(List.of(friendship));

        flushService.flush();

        verify(friendship, never()).adjustInterestScore(anyLong(), anyDouble());
        verify(friendship).adjustMutualInterestScore(10.0);

        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(friendshipRepository).batchUpdateInterestScores(captor.capture(), any());
        assertThat(captor.getValue()).hasSize(2);
    }

    private Friendship mockFriendship(String id) {
        Friendship friendship = mock(Friendship.class);
        given(friendship.getId()).willReturn(id);
        given(friendship.getMyInterestScore(USER_A)).willReturn(10.0);
        given(friendship.getMyInterestScore(USER_B)).willReturn(10.0);
        given(friendship.getIntimacy()).willReturn(0.1);
        UserReference refA = mock(UserReference.class);
        UserReference refB = mock(UserReference.class);
        given(refA.getId()).willReturn(USER_A);
        given(refB.getId()).willReturn(USER_B);
        given(friendship.getUsers()).willReturn(Set.of(refA, refB));
        return friendship;
    }
}
