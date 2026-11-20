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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class NotificationJpaRepositoryTest {

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private final Long receiverId = 100L;

    @Test
    @DisplayName("수신자의 알림을 생성 시각 최신순으로 Slice 조회한다")
    void findAllByReceiverIdOrderByCreatedAtDesc_Success_LatestFirst() {
        // given
        Notification oldest = notificationJpaRepository.save(notification(receiverId, "제목1"));
        Notification middle = notificationJpaRepository.save(notification(receiverId, "제목2"));
        Notification latest = notificationJpaRepository.save(notification(receiverId, "제목3"));
        setCreatedAt(oldest, LocalDateTime.now().minusDays(2));
        setCreatedAt(middle, LocalDateTime.now().minusDays(1));
        setCreatedAt(latest, LocalDateTime.now());

        // when
        Slice<Notification> result = notificationJpaRepository
                .findAllByReceiverIdOrderByCreatedAtDesc(receiverId, PageRequest.of(0, 2));

        // then
        assertThat(result.getContent()).extracting(Notification::getTitle).containsExactly("제목3", "제목2");
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("전체 공지와 다른 수신자의 알림은 수신자 목록에 포함되지 않는다")
    void findAllByReceiverIdOrderByCreatedAtDesc_Success_ExcludesAnnouncementAndOthers() {
        // given
        notificationJpaRepository.save(notification(receiverId, "내 알림"));
        notificationJpaRepository.save(notification(null, "전체 공지"));
        notificationJpaRepository.save(notification(200L, "남의 알림"));

        // when
        Slice<Notification> result = notificationJpaRepository
                .findAllByReceiverIdOrderByCreatedAtDesc(receiverId, PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(Notification::getTitle).containsExactly("내 알림");
    }

    @Test
    @DisplayName("미읽음 집계는 수신자의 읽지 않은 알림만 센다")
    void countByReceiverIdAndIsReadFalse_Success_CountsOnlyOwnUnread() {
        // given
        notificationJpaRepository.save(notification(receiverId, "안 읽음"));
        notificationJpaRepository.save(Notification.builder()
                .receiverId(receiverId).title("읽음").content("내용")
                .type(NotificationType.BUZZ_ARRIVAL).isRead(true).build());
        notificationJpaRepository.save(notification(null, "전체 공지"));
        notificationJpaRepository.save(notification(200L, "남의 알림"));

        // when
        long unreadCount = notificationJpaRepository.countByReceiverIdAndIsReadFalse(receiverId);

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
