package com.example.DunbarHorizon.trace.application;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.application.port.in.UserQueryUseCase;
import com.example.DunbarHorizon.global.event.notification.NotificationEvent;
import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.trace.domain.event.TraceRevealedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceEventListener {

    private final ApplicationEventPublisher eventPublisher;
    private final UserQueryUseCase userQueryUseCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTraceRevealed(TraceRevealedEvent event) {
        List<UserProfileInfo> profiles = userQueryUseCase.getUserProfiles(List.of(event.minId(), event.maxId()));

        UserProfileInfo minUser = profiles.stream()
                .filter(p -> p.id().equals(event.minId()))
                .findFirst().orElse(null);
        UserProfileInfo maxUser = profiles.stream()
                .filter(p -> p.id().equals(event.maxId()))
                .findFirst().orElse(null);

        if (minUser == null || maxUser == null) {
            log.warn("TraceRevealed 알림 발송 중단: 프로필 조회 실패 (minId={}, maxId={})", event.minId(), event.maxId());
            return;
        }

        eventPublisher.publishEvent(buildNotificationEvent(event.minId(), maxUser));
        eventPublisher.publishEvent(buildNotificationEvent(event.maxId(), minUser));
    }

    private NotificationEvent buildNotificationEvent(Long receiverId, UserProfileInfo counterpart) {
        return NotificationEvent.builder()
                .receiverId(receiverId)
                .title("서로간 잦은 방문")
                .content(counterpart.nickname() + "님과 서로 통했습니다! 방문해서 인사를 건네보세요")
                .type(NotificationType.TRACE_REVEALED)
                .metadata(Map.of(
                        "counterpartUserId", counterpart.id(),
                        "counterpartNickname", counterpart.nickname(),
                        "counterpartProfileImageUrl", counterpart.profileImage() != null ? counterpart.profileImage() : ""
                ))
                .build();
    }
}
