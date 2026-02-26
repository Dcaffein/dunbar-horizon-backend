package com.example.GooRoomBe.notification.adapter.out.persistence;

import com.example.GooRoomBe.notification.domain.Notification;
import com.example.GooRoomBe.support.MongoRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MongoRepositoryTest
class NotificationMongoRepositoryTest {

    @Autowired
    private NotificationMongoRepository notificationMongoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final Long receiverId = 100L;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.dropCollection(Notification.class);
    }

    @Test
    @DisplayName("findAllByReceiverId: 수신자 ID로 알림 목록을 최신순으로 페이징(Slice) 조회한다")
    void findAllByReceiverId_Paging_Success() {
        // given
        saveNotification(receiverId, "제목1", LocalDateTime.now().minusDays(2));
        saveNotification(receiverId, "제목2", LocalDateTime.now().minusDays(1));
        saveNotification(receiverId, "제목3", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 2);

        // when
        Slice<Notification> result = notificationMongoRepository.findAllByReceiverId(receiverId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("제목3"); // 정렬 확인
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("countByReceiverIdAndIsReadFalse: 읽지 않은 알림 개수를 카운트한다")
    void countUnreadNotifications_Success() {
        // given
        Notification n1 = Notification.builder().receiverId(receiverId).isRead(true).build();
        Notification n2 = Notification.builder().receiverId(receiverId).isRead(false).build();

        notificationMongoRepository.saveAll(List.of(n1, n2));

        // when
        long unreadCount = notificationMongoRepository.countByReceiverIdAndIsReadFalse(receiverId);

        // then
        assertThat(unreadCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("findByIsSentFalse: 전송되지 않은 알림 목록만 가져온다")
    void findUnsentNotifications_Success() {
        // given
        notificationMongoRepository.save(Notification.builder().receiverId(receiverId).isSent(true).build());
        notificationMongoRepository.save(Notification.builder().receiverId(receiverId).isSent(false).build());

        // when
        List<Notification> unsent = notificationMongoRepository.findByIsSentFalse();

        // then
        assertThat(unsent).hasSize(1);
        assertThat(unsent.get(0).isSent()).isFalse();
    }

    @Test
    @DisplayName("deleteByCreatedAtBefore: 특정 시점 이전의 알림을 삭제한다")
    void deleteOldNotifications_Success() {
        // given
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        saveNotification(receiverId, "오래된 알림", threshold.minusDays(1));
        saveNotification(receiverId, "최신 알림", threshold.plusDays(1));

        // when
        notificationMongoRepository.deleteByCreatedAtBefore(threshold);

        // then
        List<Notification> all = mongoTemplate.findAll(Notification.class);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("최신 알림");
    }

    private void saveNotification(Long receiverId, String title, LocalDateTime createdAt) {
        Notification notification = Notification.builder()
                .receiverId(receiverId)
                .title(title)
                .isSent(true)
                .build();
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        mongoTemplate.save(notification);
    }
}