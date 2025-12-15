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
        // 1. 수신자의 알림 설정(토큰) 조회
        NotificationSetting setting = settingRepository.findById(event.receiverId())
                .orElse(null);

        // 토큰이 없으면 발송 불가 (로그아웃 상태 등)
        if (setting == null || setting.getFcmToken() == null) {
            return;
        }

        // 2. 알림 수신 거부 상태면 스킵
        if (!setting.isAlarmOn()) {
            return;
        }

        // 3. 메시지 구성 및 발송
        Message message = buildMessage(setting.getFcmToken(), event);
        sendToFcm(message);
    }

    /**
     * 전체 공지 (Topic) 발송
     */
    public void sendToTopic(String topic, NotificationEvent event) {
        // 토큰 대신 Topic핑
        Message message = buildMessage(null, event, topic);
        sendToFcm(message);
    }

    // --- 내부 헬퍼 메서드 ---

    private Message buildMessage(String token, NotificationEvent event) {
        return buildMessage(token, event, null);
    }

    private Message buildMessage(String token, NotificationEvent event, String topic) {
        // 1. [Visible] 사용자에게 보이는 알림 내용
        Notification notification = Notification.builder()
                .setTitle(event.title())
                .setBody(event.content())
                .build();

        // 2. [Data] 프론트엔드 로직용 데이터 (String만 가능)
        Map<String, String> data = new HashMap<>();
        data.put("type", event.type().name());
        data.put("isNew", "true");
        if (event.relatedUrl() != null) {
            data.put("url", event.relatedUrl());
        }

        // 3. 조립
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
            // FCM 서버 오류, 토큰 만료 등
            // 여기서 에러를 던져야 Listener가 잡아서 isSent=false를 유지할 수 있음
            throw new RuntimeException("FCM 발송 실패", e);
        }
    }
}