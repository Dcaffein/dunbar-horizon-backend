package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.application.NotificationService;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({NotificationRepositoryAdapter.class, DeviceTokenRepositoryAdapter.class, NotificationService.class})
class NotificationStateConcurrencyTest {

    @Autowired private NotificationService notificationService;
    @MockitoSpyBean private NotificationRepositoryAdapter notificationRepositoryAdapter;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final Long receiverId = 9_001L;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM notifications WHERE receiver_id = ?", receiverId);
    }

    @Test
    @DisplayName("읽음이 알림을 조회한 뒤 발송 완료가 커밋돼도 읽음과 발송 완료가 모두 남는다")
    void read_KeepsSent_SentCommittedAfterReadLoaded() throws Exception {
        // given
        Notification saved = notificationService.savePendingNotification(notification());
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstFindById(loaded, resume);

        // when
        try (var workers = Executors.newSingleThreadExecutor()) {
            var read = workers.submit(() -> notificationService.readNotification(saved.getId(), receiverId));
            try {
                await(loaded);
                notificationService.markAsSent(saved.getId());
            } finally {
                resume.countDown();
            }
            read.get(10, TimeUnit.SECONDS);
        }

        // then
        assertThat(stateOf(saved.getId())).containsEntry("is_read", true).containsEntry("is_sent", true);
    }

    @Test
    @DisplayName("저장 뒤 삭제가 커밋되면 발송 완료는 삭제된 알림을 건너뛰고 나머지만 갱신한다")
    void markAllAsSent_SkipsDeleted_DeletedAfterSave() {
        // given
        List<Notification> saved = notificationService.savePendingNotifications(List.of(notification(), notification()));
        Long deletedId = saved.get(0).getId();
        Long keptId = saved.get(1).getId();
        notificationService.deleteNotification(deletedId, receiverId);

        // when
        notificationService.markAllAsSent(List.of(deletedId, keptId));

        // then
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE id = ?", Long.class, deletedId)).isZero();
        assertThat(stateOf(keptId)).containsEntry("is_sent", true);
    }

    @Test
    @DisplayName("읽음이 알림을 조회한 뒤 삭제가 커밋되면 읽음은 찾을 수 없음으로 끝나고 행을 되살리지 않는다")
    void read_NotFound_DeleteCommittedAfterReadLoaded() throws Exception {
        // given
        Notification saved = notificationService.savePendingNotification(notification());
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstFindById(loaded, resume);

        // when
        Throwable failure;
        try (var workers = Executors.newSingleThreadExecutor()) {
            var read = workers.submit(() -> notificationService.readNotification(saved.getId(), receiverId));
            try {
                await(loaded);
                notificationService.deleteNotification(saved.getId(), receiverId);
            } finally {
                resume.countDown();
            }
            failure = catchThrowable(() -> read.get(10, TimeUnit.SECONDS));
        }

        // then
        assertThat(failure).isInstanceOf(ExecutionException.class)
                .cause().isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE id = ?", Long.class, saved.getId())).isZero();
    }

    private void pauseFirstFindById(CountDownLatch loaded, CountDownLatch resume) {
        AtomicBoolean firstFind = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object found = invocation.callRealMethod();
            if (firstFind.getAndSet(false)) {
                loaded.countDown();
                await(resume);
            }
            return found;
        }).when(notificationRepositoryAdapter).findById(any());
    }

    private Map<String, Object> stateOf(Long id) {
        return jdbcTemplate.queryForMap("SELECT is_read, is_sent FROM notifications WHERE id = ?", id);
    }

    private Notification notification() {
        return Notification.builder()
                .receiverId(receiverId).title("제목").content("내용")
                .type(NotificationType.BUZZ_ARRIVAL).build();
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent notification test did not reach its checkpoint");
        }
    }
}
