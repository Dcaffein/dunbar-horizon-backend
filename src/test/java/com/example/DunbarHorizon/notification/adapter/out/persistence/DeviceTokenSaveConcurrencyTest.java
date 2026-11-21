package com.example.DunbarHorizon.notification.adapter.out.persistence;

import com.example.DunbarHorizon.notification.domain.DeviceToken;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(DeviceTokenRepositoryAdapter.class)
class DeviceTokenSaveConcurrencyTest {

    private static final String TOKEN = "token-concurrent";
    private static final int REQUESTS = 8;

    @Autowired private DeviceTokenRepositoryAdapter deviceTokenRepositoryAdapter;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM device_tokens WHERE token = ?", TOKEN);
    }

    @Test
    @DisplayName("같은 새 토큰을 동시에 저장하면 한 행만 남고 실패한 요청은 기본 키 충돌로 끝난다")
    void save_OneRowAndPrimaryKeyConflictsOnly_ConcurrentSameNewToken() throws Exception {
        // given
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Class<?>>> outcomes = new ArrayList<>();

        // when
        try (var workers = Executors.newFixedThreadPool(REQUESTS)) {
            for (long userId = 1; userId <= REQUESTS; userId++) {
                long requester = userId;
                Callable<Class<?>> save = () -> {
                    start.await();
                    try {
                        deviceTokenRepositoryAdapter.save(new DeviceToken(requester, TOKEN));
                        return Void.class;
                    } catch (RuntimeException failure) {
                        return failure.getClass();
                    }
                };
                outcomes.add(workers.submit(save));
            }
            start.countDown();

            // then
            for (Future<Class<?>> outcome : outcomes) {
                assertThat(outcome.get(10, TimeUnit.SECONDS))
                        .isIn(Void.class, DataIntegrityViolationException.class);
            }
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device_tokens WHERE token = ?", Long.class, TOKEN)).isEqualTo(1L);
    }
}
