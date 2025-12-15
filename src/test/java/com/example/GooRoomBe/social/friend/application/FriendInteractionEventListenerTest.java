package com.example.GooRoomBe.social.friend.application;

import com.example.GooRoomBe.global.event.InteractionType;
import com.example.GooRoomBe.global.event.UserInteractionEvent;
import com.example.GooRoomBe.social.friend.domain.Friendship;
import com.example.GooRoomBe.social.friend.domain.FriendshipPort;
import com.example.GooRoomBe.social.friend.exception.FriendshipNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendInteractionEventListenerTest {

    @Mock
    private FriendshipPort friendshipPort;

    @InjectMocks
    private FriendInteractionEventListener listener;

    private final String ACTOR_ID = "actor-id";
    private final String TARGET_ID = "target-id";

    @Test
    @DisplayName("상호작용 이벤트 수신 시: 친구 관계가 존재하면 점수를 조정하고 저장한다")
    void handleUserInteraction_Success() {
        // Given
        UserInteractionEvent event = new UserInteractionEvent(
                ACTOR_ID,
                TARGET_ID,
                InteractionType.VISIT,
                1.0
        );

        Friendship friendship = mock(Friendship.class);

        given(friendshipPort.getFriendship(ACTOR_ID, TARGET_ID)).willReturn(friendship);

        // When
        listener.handleUserInteraction(event);

        // Then
        // 1. 도메인 엔티티의 비즈니스 메서드가 호출되었는지 검증
        verify(friendship).adjustInterestScore(ACTOR_ID, 1.0);

        // 2. 변경된 엔티티가 저장소로 넘어갔는지 검증
        verify(friendshipPort).save(friendship);
    }

    @Test
    @DisplayName("상호작용 이벤트 수신 시: 친구 관계가 없으면(예외 발생) 무시하고 저장을 수행하지 않는다")
    void handleUserInteraction_NotFound_ShouldIgnore() {
        // Given
        UserInteractionEvent event = new UserInteractionEvent(
                ACTOR_ID,
                TARGET_ID,
                InteractionType.VISIT,
                1.0
        );

        // Port가 예외를 던지도록 설정
        given(friendshipPort.getFriendship(ACTOR_ID, TARGET_ID))
                .willThrow(new FriendshipNotFoundException(ACTOR_ID, TARGET_ID));

        // When
        listener.handleUserInteraction(event);

        // Then
        // 1. 예외가 catch 블록에서 잡혔으므로 테스트는 실패하지 않아야 함
        // 2. 저장 메서드는 절대 호출되지 않아야 함
        verify(friendshipPort, never()).save(any());
    }

    @Test
    @DisplayName("기타 예외 발생 시: 로그를 남기고 안전하게 종료된다 (저장 호출 안 함)")
    void handleUserInteraction_UnexpectedError_ShouldLogAndExit() {
        // Given
        UserInteractionEvent event = new UserInteractionEvent(ACTOR_ID, TARGET_ID, InteractionType.VISIT, 1.0);

        // DB 연결 오류 등 예상치 못한 예외 발생 설정
        given(friendshipPort.getFriendship(anyString(), anyString()))
                .willThrow(new RuntimeException("DB Connection Error"));

        // When
        listener.handleUserInteraction(event);

        // Then
        verify(friendshipPort, never()).save(any());
    }
}