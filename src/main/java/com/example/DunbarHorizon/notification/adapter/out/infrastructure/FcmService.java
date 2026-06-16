package com.example.DunbarHorizon.notification.adapter.out.infrastructure;

import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmService implements NotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public List<String> sendMulticast(NotificationEvent event, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return Collections.emptyList();

        List<String> invalidTokens = new ArrayList<>();

        Notification fcmNotification = createFcmNotification(event);
        Map<String, String> fcmData = createDataPayload(event);

        for (int i = 0; i < tokens.size(); i += 500) {
            List<String> chunk = tokens.subList(i, Math.min(i + 500, tokens.size()));

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(fcmNotification)
                    .putAllData(fcmData)
                    .addAllTokens(chunk)
                    .build();

            invalidTokens.addAll(dispatchMulticastChunk(message, chunk));
        }
        return invalidTokens;
    }

    @Override
    public void sendBroadcast(NotificationEvent event) {
        // 단건 전송용 Message 빌드
        Message message = Message.builder()
                .setNotification(createFcmNotification(event))
                .putAllData(createDataPayload(event))
                .setTopic("notice")
                .build();
        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 브로드캐스트 전송 실패: {}", e.getMessage());
        }
    }

    @Override
    public void subscribeToTopic(String token, String topic) {
        try {
            firebaseMessaging.subscribeToTopic(Collections.singletonList(token), topic);
        } catch (Exception e) {
            log.error("토픽 구독 실패: {}", e.getMessage());
        }
    }

    @Override
    public void unsubscribeFromTopic(String token, String topic) {
        try {
            firebaseMessaging.unsubscribeFromTopic(Collections.singletonList(token), topic);
        } catch (Exception e) {
            log.error("토픽 해지 실패: {}", e.getMessage());
        }
    }

    // --- 내부 헬퍼 메서드 ---

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
                        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
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

    // Builder 반환 대신 Notification 객체 자체를 반환하도록 분리
    private Notification createFcmNotification(NotificationEvent event) {
        return Notification.builder()
                .setTitle(event.title())
                .setBody(event.content())
                .build();
    }

    // Builder 반환 대신 Data Map 자체를 반환하도록 분리
    private Map<String, String> createDataPayload(NotificationEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("type", event.type().name());
        if (event.metadata() != null) {
            event.metadata().forEach((key, value) -> data.put(key, String.valueOf(value)));
        }
        return data;
    }
}