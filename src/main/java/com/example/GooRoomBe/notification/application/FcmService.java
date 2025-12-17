package com.example.GooRoomBe.notification.application;

import com.example.GooRoomBe.global.event.NotificationEvent;
import com.example.GooRoomBe.notification.domain.NotificationSetting;
import com.example.GooRoomBe.notification.repository.NotificationSettingRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final NotificationSettingRepository settingRepository;

    /**
     * 개별 사용자에게 알림 발송
     */
    public void sendNotification(NotificationEvent event) {
        // 수신자의 알림 설정(토큰) 조회
        NotificationSetting setting = settingRepository.findById(event.receiverId())
                .orElse(null);

        // 토큰이 없으면 발송 불가
        if (setting == null || setting.getFcmToken() == null) {
            return;
        }

        //  알림 수신 거부 상태면 스킵
        if (!setting.isAlarmOn()) {
            return;
        }

        Message message = buildMessage(setting.getFcmToken(), event);
        sendToFcm(message);
    }

    /**
     * 전체 공지 (Topic) 발송
     */
    public void sendToTopic(String topic, NotificationEvent event) {
        Message message = buildMessage(null, event, topic);
        sendToFcm(message);
    }

    private Message buildMessage(String token, NotificationEvent event) {
        return buildMessage(token, event, null);
    }

    private Message buildMessage(String token, NotificationEvent event, String topic) {
        Notification notification = Notification.builder()
                .setTitle(event.title())
                .setBody(event.content())
                .build();

        Map<String, String> data = new HashMap<>();
        data.put("type", event.type().name());
        data.put("isNew", "true");
        if (event.relatedUrl() != null) {
            data.put("url", event.relatedUrl());
        }

        Message.Builder builder = Message.builder()
                .setNotification(notification)
                .putAllData(data);

        if (token != null) builder.setToken(token);
        if (topic != null) builder.setTopic(topic);

        return builder.build();
    }

    private void sendToFcm(Message message) {
        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            throw new RuntimeException("FCM 발송 실패", e);
        }
    }
}