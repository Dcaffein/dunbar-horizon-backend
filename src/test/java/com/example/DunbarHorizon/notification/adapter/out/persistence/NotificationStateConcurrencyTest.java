package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.application.DeviceTokenService;
import com.example.DunbarHorizon.notification.application.NotificationService;
import com.example.DunbarHorizon.notification.application.port.out.NotificationSender;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
@Import({NotificationRepositoryAdapter.class, DeviceTokenRepositoryAdapter.class, DeviceTokenService.class, NotificationService.class})
class NotificationStateConcurrencyTest {

    @Autowired private NotificationService notificationService;
    @MockitoSpyBean private NotificationRepositoryAdapter notificationRepositoryAdapter;
    @MockitoBean private NotificationSender notificationSender;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final Long receiverId = 9_001L;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM notifications WHERE receiver_id = ?", receiverId);
    }

    @Test
    @DisplayName("읽음이 알림을 조회한 뒤 삭제가 커밋되면 읽음은 동시 수정 실패로 끝나고 행을 되살리지 않는다")
    void read_ConcurrentModification_DeleteCommittedAfterReadLoaded() throws Exception {
        // given
        Notification saved = notificationRepositoryAdapter.saveAll(List.of(notification())).get(0);
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstRetainedLookup(loaded, resume);

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
                .cause().isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE id = ?", Long.class, saved.getId())).isZero();
    }

    @Test
    @DisplayName("삭제가 알림을 조회한 뒤 다른 삭제가 커밋돼도 늦은 삭제는 성공으로 끝나고 행은 지워진 채 남는다")
    void delete_Succeeds_OtherDeleteCommittedAfterLoaded() throws Exception {
        // given
        Notification saved = notificationRepositoryAdapter.saveAll(List.of(notification())).get(0);
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        pauseFirstLookup(loaded, resume);

        // when
        Throwable failure;
        try (var workers = Executors.newSingleThreadExecutor()) {
            var lateDelete = workers.submit(() -> notificationService.deleteNotification(saved.getId(), receiverId));
            try {
                await(loaded);
                notificationService.deleteNotification(saved.getId(), receiverId);
            } finally {
                resume.countDown();
            }
            failure = catchThrowable(() -> lateDelete.get(10, TimeUnit.SECONDS));
        }

        // then
        assertThat(failure).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE id = ?", Long.class, saved.getId())).isZero();
    }

    private void pauseFirstLookup(CountDownLatch loaded, CountDownLatch resume) {
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

    private void pauseFirstRetainedLookup(CountDownLatch loaded, CountDownLatch resume) {
        AtomicBoolean firstFind = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Object found = invocation.callRealMethod();
            if (firstFind.getAndSet(false)) {
                loaded.countDown();
                await(resume);
            }
            return found;
        }).when(notificationRepositoryAdapter).findRetainedById(any(), any());
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
