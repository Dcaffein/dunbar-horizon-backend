package com.example.DunbarHorizon.notification.adapter.out.infrastructure;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @InjectMocks
    private FcmService fcmService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Test
    @DisplayName("sendMulticast: 등록이 풀렸거나 발신자가 다른 토큰만 무효로 돌려주고 잘못된 요청 실패는 무효로 보지 않는다")
    void sendMulticast_무효_토큰은_등록_해제와_발신자_불일치뿐이다() throws Exception {
        // given
        List<String> tokens = List.of("ok", "unregistered", "sender-mismatch", "invalid-argument");
        List<SendResponse> responses = List.of(
                success(),
                failure(MessagingErrorCode.UNREGISTERED),
                failure(MessagingErrorCode.SENDER_ID_MISMATCH),
                failure(MessagingErrorCode.INVALID_ARGUMENT));
        BatchResponse batch = mock(BatchResponse.class);
        given(batch.getFailureCount()).willReturn(3);
        given(batch.getResponses()).willReturn(responses);
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(batch);

        // when
        List<String> invalidTokens = fcmService.sendMulticast(event(), tokens);

        // then
        assertThat(invalidTokens).containsExactlyInAnyOrder("unregistered", "sender-mismatch");
    }

    private static SendResponse success() {
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(true);
        return response;
    }

    private static SendResponse failure(MessagingErrorCode code) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(code);
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(false);
        given(response.getException()).willReturn(exception);
        return response;
    }

    private static NotificationEvent event() {
        return NotificationEvent.builder()
                .receiverId(1L).title("제목").content("내용")
                .type(NotificationType.BUZZ_ARRIVAL).build();
    }
}
