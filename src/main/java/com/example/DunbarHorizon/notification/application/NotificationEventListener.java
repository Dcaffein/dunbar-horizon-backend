package com.example.DunbarHorizon.notification.application;

import com.example.DunbarHorizon.global.event.DeviceTokenDeregisteredEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNotificationRequest(NotificationEvent event) {
        notificationService.send(event.receiverIds(), event.title(), event.content(), event.type(), event.metadata());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDeviceTokenDeregistration(DeviceTokenDeregisteredEvent event) {
        deviceTokenService.removeDeviceToken(event.token());
    }
}
