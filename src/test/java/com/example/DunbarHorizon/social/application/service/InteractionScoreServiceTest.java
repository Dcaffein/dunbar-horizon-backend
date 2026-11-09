package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.ScoreDirection;
import com.example.DunbarHorizon.social.domain.friend.FriendRecognition;
import com.example.DunbarHorizon.social.domain.friend.FriendTestFactory;
import com.example.DunbarHorizon.social.domain.friend.Friendship;
import com.example.DunbarHorizon.social.domain.friend.InteractionScorePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionScoreServiceTest {

    private static final Long FLAG_ID = 100L;
    private static final Long HOST_ID = 10L;
    private static final List<Long> PARTICIPANT_IDS = List.of(21L, 22L);
    private static final InteractionType FLAG_TYPE = InteractionType.FLAG_ENDED;
    private static final double FLAG_DELTA = InteractionScorePolicy.scoreOf(FLAG_TYPE);
    private static final SocialUser USER_1 = new SocialUser(1L, "사용자1", null);
    private static final SocialUser USER_2 = new SocialUser(2L, "사용자2", null);

    @InjectMocks private InteractionScoreService interactionScoreService;
    @Mock private FriendshipRepository friendshipRepository;

    @Test
    @DisplayName("수정용으로 불러온 관계를 계산한 뒤 쓴다")
    void applyInteraction_LoadsForUpdateThenSaves() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(USER_1, USER_2);
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection())).thenReturn(List.of(friendship));

        // when
        interactionScoreService.applyInteraction(1L, 2L, InteractionType.FLAG_ENDED);

        // then
        InOrder order = inOrder(friendshipRepository);
        order.verify(friendshipRepository).findAllByIdsForUpdate(Set.of("1_2"));
        order.verify(friendshipRepository).saveInterestScores(List.of(friendship));
    }

    @Test
    @DisplayName("mutual 타입은 양쪽 점수를 타입에 맞는 델타로 올린다")
    void applyInteraction_AdjustsBothSides_MutualType() {
        // given
        InteractionType type = InteractionType.FLAG_ENDED_ENCORE;
        Friendship friendship = FriendTestFactory.createFriendship(USER_1, USER_2);
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection())).thenReturn(List.of(friendship));

        // when
        interactionScoreService.applyInteraction(2L, 1L, type);

        // then
        double expected = FriendRecognition.INITIAL_RAW_SCORE + InteractionScorePolicy.scoreOf(type);
        assertThat(friendship.getMyInterestScore(1L)).isCloseTo(expected, within(0.0001));
        assertThat(friendship.getMyInterestScore(2L)).isCloseTo(expected, within(0.0001));
        assertThat(friendship.getIntimacy()).isCloseTo(FriendRecognition.normalize(expected), within(0.0001));
    }

    @Test
    @DisplayName("단방향 타입은 행위자 쪽 점수만 올린다")
    void applyInteraction_AdjustsOnlyActorSide_UnilateralType() {
        // given
        InteractionType type = InteractionType.VISIT;
        Friendship friendship = FriendTestFactory.createFriendship(USER_1, USER_2);
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection())).thenReturn(List.of(friendship));

        // when
        interactionScoreService.applyInteraction(2L, 1L, type);

        // then
        assertThat(friendship.getMyInterestScore(2L))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + InteractionScorePolicy.scoreOf(type), within(0.0001));
        assertThat(friendship.getMyInterestScore(1L)).isCloseTo(FriendRecognition.INITIAL_RAW_SCORE, within(0.0001));
    }

    @Test
    @DisplayName("Flag 결론은 호스트-참가자와 참가자끼리의 쌍 전체를 한 번에 불러온다")
    void applyFlagConclusion_LoadsEveryPairOnce() {
        // given
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection())).thenReturn(List.of());

        // when
        interactionScoreService.applyFlagConclusion(FLAG_ID, HOST_ID, PARTICIPANT_IDS, FLAG_TYPE, ScoreDirection.GRANT);

        // then
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(friendshipRepository).findAllByIdsForUpdate(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder("10_21", "10_22", "21_22");
    }

    @Test
    @DisplayName("친구가 아니어서 불러오지 못한 쌍은 건너뛰고 불러온 관계만 갱신한다")
    void applyFlagConclusion_SkipsMissingPairs() {
        // given
        Friendship friendship = FriendTestFactory.createFriendship(USER_1, USER_2);
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection())).thenReturn(List.of(friendship));

        // when
        interactionScoreService.applyFlagConclusion(FLAG_ID, 1L, List.of(2L, 3L), FLAG_TYPE, ScoreDirection.GRANT);

        // then
        verify(friendshipRepository).saveInterestScores(List.of(friendship));
        assertThat(friendship.getMyInterestScore(1L))
                .isCloseTo(FriendRecognition.INITIAL_RAW_SCORE + FLAG_DELTA, within(0.0001));
    }

    @Test
    @DisplayName("REVOKE는 같은 크기만큼 점수를 내린다")
    void applyFlagConclusion_LowersScore_Revoke() {
        // given
        InteractionType type = InteractionType.FLAG_PRESERVED;
        Friendship friendship = FriendTestFactory.createFriendship(USER_1, USER_2);
        when(friendshipRepository.findAllByIdsForUpdate(anyCollection())).thenReturn(List.of(friendship));

        // when
        interactionScoreService.applyFlagConclusion(FLAG_ID, 1L, List.of(2L), type, ScoreDirection.REVOKE);

        // then
        double expected = Math.max(0.0, FriendRecognition.INITIAL_RAW_SCORE - InteractionScorePolicy.scoreOf(type));
        assertThat(friendship.getMyInterestScore(1L)).isCloseTo(expected, within(0.0001));
        assertThat(friendship.getMyInterestScore(2L)).isCloseTo(expected, within(0.0001));
    }
}
