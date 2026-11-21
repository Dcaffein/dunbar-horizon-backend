package com.example.DunbarHorizon.notification.adapter.out.fcm;

import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.application.port.out.PushMessage;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmAdapter implements NotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public List<String> send(PushMessage message, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return Collections.emptyList();

        List<String> invalidTokens = new ArrayList<>();

        Notification fcmNotification = createFcmNotification(message);
        Map<String, String> fcmData = createDataPayload(message);

        for (int i = 0; i < tokens.size(); i += 500) {
            List<String> chunk = tokens.subList(i, Math.min(i + 500, tokens.size()));

            MulticastMessage multicastMessage = MulticastMessage.builder()
                    .setNotification(fcmNotification)
                    .putAllData(fcmData)
                    .addAllTokens(chunk)
                    .build();

            invalidTokens.addAll(dispatchMulticastChunk(multicastMessage, chunk));
        }
        return invalidTokens;
    }

    private List<String> dispatchMulticastChunk(MulticastMessage message, List<String> chunk) {
        List<String> invalidTokensInChunk = new ArrayList<>();
        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        MessagingErrorCode code = responses.get(i).getException().getMessagingErrorCode();
                        log.warn("FCM 토큰 전송 실패 [{}]: {}", code, chunk.get(i));
                        if (code == MessagingErrorCode.UNREGISTERED
                                || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
                            invalidTokensInChunk.add(chunk.get(i));
                        }
                    }
                }
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM 멀티캐스트 청크 에러: {}", e.getMessage());
        }
        return invalidTokensInChunk;
    }

    private Notification createFcmNotification(PushMessage message) {
        return Notification.builder()
                .setTitle(message.title())
                .setBody(message.body())
                .build();
    }

    private Map<String, String> createDataPayload(PushMessage message) {
        Map<String, String> data = new HashMap<>();
        message.data().forEach((key, value) -> data.put(key, String.valueOf(value)));
        return data;
    }
}
