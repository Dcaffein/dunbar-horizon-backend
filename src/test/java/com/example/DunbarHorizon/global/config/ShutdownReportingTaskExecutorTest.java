package com.example.DunbarHorizon.global.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ShutdownReportingTaskExecutorTest {

    private static final long DRAIN_MILLIS = 200L;

    private Logger executorLogger;
    private ListAppender<ILoggingEvent> logAppender;
    private ShutdownReportingTaskExecutor executor;

    @BeforeEach
    void setUp() {
        executorLogger = (Logger) LoggerFactory.getLogger(ShutdownReportingTaskExecutor.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        executorLogger.addAppender(logAppender);

        executor = new ShutdownReportingTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationMillis(DRAIN_MILLIS);
        executor.initialize();
    }

    @AfterEach
    void tearDown() {
        executorLogger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("드레인 시간 안에 비우지 못한 작업 수를 로그에 남긴다")
    void 드레인_시간_안에_비우지_못한_작업_수를_로그에_남긴다() throws Exception {
        // given
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        executor.execute(() -> {
            running.countDown();
            await(release);
        });
        assertThat(running.await(1, TimeUnit.SECONDS)).isTrue();
        int queuedCount = 3;
        for (int i = 0; i < queuedCount; i++) {
            executor.execute(() -> {});
        }

        // when
        executor.shutdown();

        // then
        assertThat(errorMessages())
                .singleElement()
                .asString()
                .contains(String.valueOf(queuedCount));

        release.countDown();
    }

    @Test
    @DisplayName("드레인 안에 모두 비우면 아무것도 남기지 않는다")
    void 드레인_안에_모두_비우면_아무것도_남기지_않는다() {
        // given
        executor.execute(() -> {});

        // when
        executor.shutdown();

        // then
        assertThat(errorMessages()).isEmpty();
    }

    /**
     * 같은 로거로 스프링이 드레인 타임아웃 WARN을 남기므로 ERROR만 본다.
     */
    private List<String> errorMessages() {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
