package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class NotificationJpaRepositoryTest {

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private final Long receiverId = 100L;
    private final LocalDateTime cutoff = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final LocalDateTime justAfterCutoff = cutoff.plusNanos(1_000);

    @Test
    @DisplayName("수신자의 알림을 생성 시각 최신순으로 Slice 조회한다")
    void findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc_Success_LatestFirst() {
        // given
        Notification oldest = notificationJpaRepository.save(notification(receiverId, "제목1"));
        Notification middle = notificationJpaRepository.save(notification(receiverId, "제목2"));
        Notification latest = notificationJpaRepository.save(notification(receiverId, "제목3"));
        setCreatedAt(oldest, LocalDateTime.now().minusDays(2));
        setCreatedAt(middle, LocalDateTime.now().minusDays(1));
        setCreatedAt(latest, LocalDateTime.now());

        // when
        Slice<Notification> result = notificationJpaRepository
                .findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc(receiverId, cutoff, PageRequest.of(0, 2));

        // then
        assertThat(result.getContent()).extracting(Notification::getTitle).containsExactly("제목3", "제목2");
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("다른 수신자의 알림은 수신자 목록에 포함되지 않는다")
    void findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc_Success_ExcludesOthers() {
        // given
        notificationJpaRepository.save(notification(receiverId, "내 알림"));
        notificationJpaRepository.save(notification(200L, "남의 알림"));

        // when
        Slice<Notification> result = notificationJpaRepository
                .findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc(receiverId, cutoff, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Notification::getTitle).containsExactly("내 알림");
    }

    @Test
    @DisplayName("미읽음 집계는 수신자의 읽지 않은 알림만 센다")
    void countByReceiverIdAndIsReadFalseAndCreatedAtAfter_Success_CountsOnlyOwnUnread() {
        // given
        notificationJpaRepository.save(notification(receiverId, "안 읽음"));
        notificationJpaRepository.save(Notification.builder()
                .receiverId(receiverId).title("읽음").content("내용")
                .type(NotificationType.BUZZ_ARRIVAL).isRead(true).build());
        notificationJpaRepository.save(notification(200L, "남의 알림"));

        // when
        long unreadCount = notificationJpaRepository.countByReceiverIdAndIsReadFalseAndCreatedAtAfter(receiverId, cutoff);

        // then
        assertThat(unreadCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("저장한 알림은 ID와 생성 시각을 받고 메타데이터를 같은 JSON으로 돌려준다")
    void save_Success_RoundTripsMetadataAsSameJson() throws Exception {
        // given
        Map<String, Object> metadata = Map.of(
                "flagId", 7L,
                "counterpartUserId", 3_000_000_000L,
                "buzzId", "65f1c2a9e4b0a1b2c3d4e5f6",
                "counterpartProfileImageUrl", ""
        );
        Notification saved = notificationJpaRepository.save(Notification.builder()
                .receiverId(receiverId).title("제목").content("내용")
                .type(NotificationType.FLAG_INVITATION).metadata(metadata).build());
        entityManager.flush();
        entityManager.clear();

        // when
        Notification found = notificationJpaRepository.findById(saved.getId()).orElseThrow();

        // then
        ObjectMapper objectMapper = new ObjectMapper();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getType()).isEqualTo(NotificationType.FLAG_INVITATION);
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(found.getMetadata())))
                .isEqualTo(objectMapper.readTree(objectMapper.writeValueAsString(metadata)));
    }

    @Test
    @DisplayName("목록은 기준선과 같은 시각에 생성된 알림을 빼고 기준선 직후 알림만 담는다")
    void findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc_Boundary_ExcludesAtCutoff() {
        // given
        Notification atCutoff = notificationJpaRepository.save(notification(receiverId, "기준선"));
        Notification afterCutoff = notificationJpaRepository.save(notification(receiverId, "기준선 직후"));
        setCreatedAt(atCutoff, cutoff);
        setCreatedAt(afterCutoff, justAfterCutoff);

        // when
        Slice<Notification> result = notificationJpaRepository
                .findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDesc(receiverId, cutoff, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Notification::getTitle).containsExactly("기준선 직후");
    }

    @Test
    @DisplayName("미읽음 집계는 기준선과 같은 시각에 생성된 알림을 세지 않는다")
    void countByReceiverIdAndIsReadFalseAndCreatedAtAfter_Boundary_ExcludesAtCutoff() {
        // given
        Notification atCutoff = notificationJpaRepository.save(notification(receiverId, "기준선"));
        Notification afterCutoff = notificationJpaRepository.save(notification(receiverId, "기준선 직후"));
        setCreatedAt(atCutoff, cutoff);
        setCreatedAt(afterCutoff, justAfterCutoff);

        // when
        long unreadCount = notificationJpaRepository.countByReceiverIdAndIsReadFalseAndCreatedAtAfter(receiverId, cutoff);

        // then
        assertThat(unreadCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("단건 조회는 기준선과 같은 시각에 생성된 알림을 찾지 않는다")
    void findByIdAndCreatedAtAfter_Boundary_ExcludesAtCutoff() {
        // given
        Notification atCutoff = notificationJpaRepository.save(notification(receiverId, "기준선"));
        Notification afterCutoff = notificationJpaRepository.save(notification(receiverId, "기준선 직후"));
        setCreatedAt(atCutoff, cutoff);
        setCreatedAt(afterCutoff, justAfterCutoff);

        // when & then
        assertThat(notificationJpaRepository.findByIdAndCreatedAtAfter(atCutoff.getId(), cutoff)).isEmpty();
        assertThat(notificationJpaRepository.findByIdAndCreatedAtAfter(afterCutoff.getId(), cutoff)).isPresent();
    }

    @Test
    @DisplayName("정리는 기준선과 같거나 이전에 생성된 알림을 지우고 기준선 직후 알림은 남긴다")
    void deleteAllByCreatedAtAtOrBefore_Boundary_DeletesAtCutoffKeepsAfter() {
        // given
        Notification beforeCutoff = notificationJpaRepository.save(notification(receiverId, "기준선 이전"));
        Notification atCutoff = notificationJpaRepository.save(notification(200L, "기준선"));
        Notification afterCutoff = notificationJpaRepository.save(notification(receiverId, "기준선 직후"));
        setCreatedAt(beforeCutoff, cutoff.minusDays(1));
        setCreatedAt(atCutoff, cutoff);
        setCreatedAt(afterCutoff, justAfterCutoff);

        // when
        int deleted = notificationJpaRepository.deleteAllByCreatedAtAtOrBefore(cutoff);

        // then
        assertThat(deleted).isGreaterThanOrEqualTo(2);
        assertThat(notificationJpaRepository.findAllById(
                List.of(beforeCutoff.getId(), atCutoff.getId(), afterCutoff.getId())))
                .extracting(Notification::getTitle)
                .containsExactly("기준선 직후");
    }

    private Notification notification(Long receiverId, String title) {
        return Notification.builder()
                .receiverId(receiverId)
                .title(title)
                .content("내용")
                .type(NotificationType.BUZZ_ARRIVAL)
                .build();
    }

    private void setCreatedAt(Notification notification, LocalDateTime createdAt) {
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE notifications SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", notification.getId())
                .executeUpdate();
        entityManager.clear();
    }
}
