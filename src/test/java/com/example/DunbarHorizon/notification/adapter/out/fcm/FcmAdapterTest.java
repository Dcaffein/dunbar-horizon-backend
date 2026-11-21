package com.example.DunbarHorizon.notification.adapter.out.fcm;

import com.example.DunbarHorizon.notification.application.port.out.PushMessage;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmAdapterTest {

    @InjectMocks
    private FcmAdapter fcmAdapter;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Test
    @DisplayName("send: 등록이 풀렸거나 발신자가 다른 토큰만 무효로 돌려주고 잘못된 요청 실패는 무효로 보지 않는다")
    void send_무효_토큰은_등록_해제와_발신자_불일치뿐이다() throws Exception {
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
        List<String> invalidTokens = fcmAdapter.send(message(), tokens);

        // then
        assertThat(invalidTokens).containsExactlyInAnyOrder("unregistered", "sender-mismatch");
    }

    @Test
    @DisplayName("send: data 값을 문자열로 바꿔 담는다")
    void send_data_값을_문자열로_바꾼다() throws Exception {
        // given
        BatchResponse batch = mock(BatchResponse.class);
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(batch);
        PushMessage message = new PushMessage("제목", "내용", Map.of("type", "FLAG_INVITATION", "flagId", 7L));

        // when
        fcmAdapter.send(message, List.of("token"));

        // then
        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebaseMessaging).sendEachForMulticast(captor.capture());
        assertThat(captor.getValue()).extracting("data")
                .isEqualTo(Map.of("type", "FLAG_INVITATION", "flagId", "7"));
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

    private static PushMessage message() {
        return new PushMessage("제목", "내용", Map.of("type", "BUZZ_ARRIVAL"));
    }
}
