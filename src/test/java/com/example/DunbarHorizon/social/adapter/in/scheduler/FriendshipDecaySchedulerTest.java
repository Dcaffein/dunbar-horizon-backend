package com.example.DunbarHorizon.social.adapter.in.scheduler;

import com.example.DunbarHorizon.social.application.service.FriendshipDecayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FriendshipDecaySchedulerTest {

    private static final String CRON = "0 0 3 * * *";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingConfig.class);

    @Test
    @DisplayName("cron 설정값이 없으면 감쇄가 스케줄에 등록되지 않는다")
    void runDecay_NotScheduled_NoCronProperty() {
        contextRunner.run(context -> assertThat(scheduledTasks(context)).isEmpty());
    }

    @Test
    @DisplayName("cron 설정값이 있으면 그 주기로 감쇄가 등록된다")
    void runDecay_Scheduled_CronProperty() {
        contextRunner
                .withPropertyValues("friendship.decay.cron=" + CRON)
                .run(context -> assertThat(scheduledTasks(context))
                        .singleElement()
                        .extracting(scheduled -> ((CronTask) scheduled.getTask()).getExpression())
                        .isEqualTo(CRON));
    }

    private Set<ScheduledTask> scheduledTasks(ApplicationContext context) {
        return context.getBean(ScheduledTaskHolder.class).getScheduledTasks();
    }

    @Configuration
    @EnableScheduling
    @Import(FriendshipDecayScheduler.class)
    static class SchedulingConfig {

        @Bean
        FriendshipDecayService friendshipDecayService() {
            return mock(FriendshipDecayService.class);
        }
    }
}
