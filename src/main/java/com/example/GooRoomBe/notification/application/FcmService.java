package com.example.GooRoomBe.notification.application;

import com.example.GooRoomBe.global.event.notification.NotificationEvent;
import com.example.GooRoomBe.notification.domain.NotificationSetting;
import com.example.GooRoomBe.notification.repository.NotificationRepository;
import com.example.GooRoomBe.notification.repository.NotificationSettingRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationSettingRepository settingRepository;
    private final NotificationRepository notificationRepository;

    public void sendMulticastNotification(NotificationEvent event, List<com.example.GooRoomBe.notification.domain.Notification> savedNotifications) {
        List<NotificationSetting> settings = settingRepository.findAllByUserIdIn(event.receiverIds());

        Map<String, Long> tokenToUserMap = settings.stream()
                .filter(s -> s.isAlarmOn() && s.getFcmToken() != null)
                .collect(Collectors.toMap(NotificationSetting::getFcmToken, NotificationSetting::getUserId, (a, b) -> a));

        List<String> tokens = new ArrayList<>(tokenToUserMap.keySet());
        if (tokens.isEmpty()) return;

        for (int i = 0; i < tokens.size(); i += 500) {
            List<String> chunk = tokens.subList(i, Math.min(i + 500, tokens.size()));
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(createFcmNotification(event))
                    .putAllData(createDataPayload(event))
                    .addAllTokens(chunk)
                    .build();

            dispatchMulticast(message, chunk, tokenToUserMap, savedNotifications);
        }
    }

    public void broadcastNotification(NotificationEvent event, String notificationId) {
        Message message = Message.builder()
                .setNotification(createFcmNotification(event))
                .putAllData(createDataPayload(event))
                .setTopic("notice")
                .build();
        dispatchToBroadcast(message, notificationId);
    }

    public void subscribeToTopic(String token, String topic) {
        try {
            firebaseMessaging.subscribeToTopic(Collections.singletonList(token), topic);
            log.info("토픽 구독 성공: {}, 토큰: {}", topic, token);
        } catch (Exception e) {
            log.error("토픽 구독 실패: {}", e.getMessage());
        }
    }

    public void unsubscribeFromTopic(String token, String topic) {
        try {
            firebaseMessaging.unsubscribeFromTopic(Collections.singletonList(token), topic);
            log.info("토픽 구독 해지 성공: {}, 토큰: {}", topic, token);
        } catch (Exception e) {
            log.error("토픽 구독 해지 실패: {}", e.getMessage());
        }
    }

    @Async
    public void dispatchMulticast(MulticastMessage message, List<String> tokens,
                                  Map<String, Long> tokenToUserMap,
                                  List<com.example.GooRoomBe.notification.domain.Notification> savedNotifications) {
        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            if (response.getFailureCount() > 0) {
                handlePartialFailures(response, tokens, tokenToUserMap);
            }
            updateBulkSentStatus(savedNotifications);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 멀티캐스트 에러: {}", e.getMessage());
        }
    }

    @Async
    public void dispatchToBroadcast(Message message, String notificationId) {
        try {
            firebaseMessaging.send(message);
            updateSentStatus(notificationId);
        } catch (FirebaseMessagingException e) {
            log.error("토픽 전송 실패: {}", e.getMessage());
        }
    }

    private Notification createFcmNotification(NotificationEvent event) {
        return Notification.builder()
                .setTitle(event.title())
                .setBody(event.content())
                .build();
    }

    private Map<String, String> createDataPayload(NotificationEvent event) {
        Map<String, String> data = new HashMap<>();
        data.put("type", event.type().name());
        if (event.metadata() != null) {
            event.metadata().forEach((key, value) -> data.put(key, String.valueOf(value)));
        }
        return data;
    }

    private void handlePartialFailures(BatchResponse response, List<String> tokens, Map<String, Long> tokenToUserMap) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                MessagingErrorCode code = responses.get(i).getException().getMessagingErrorCode();
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    handleInvalidToken(tokenToUserMap.get(tokens.get(i)));
                }
            }
        }
    }

    private void handleInvalidToken(Long userId) {
        if (userId == null) return;
        settingRepository.findById(userId).ifPresent(setting -> {
            setting.updateToken(null);
            settingRepository.save(setting);
        });
    }

    private void updateSentStatus(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.markAsSent();
            notificationRepository.save(n);
        });
    }

    private void updateBulkSentStatus(List<com.example.GooRoomBe.notification.domain.Notification> notifications) {
        notifications.forEach(com.example.GooRoomBe.notification.domain.Notification::markAsSent);
        notificationRepository.saveAll(notifications);
    }
}