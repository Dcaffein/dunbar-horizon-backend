package com.example.DunbarHorizon.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
            .withUserConfiguration(AsyncConfig.class);

    @Test
    @DisplayName("@Async 공용 익스큐터가 종료 시 미처리 건수를 남기는 구현 하나로 등록된다")
    void async_공용_익스큐터가_종료_시_미처리_건수를_남기는_구현_하나로_등록된다() {
        contextRunner.run(context -> {
            // then
            assertThat(context).hasSingleBean(Executor.class);
            assertThat(context.getBean("applicationTaskExecutor"))
                    .isInstanceOf(ShutdownReportingTaskExecutor.class)
                    .isSameAs(context.getBean("taskExecutor"));
        });
    }

    @Test
    @DisplayName("application.yml의 종료 드레인 설정이 공용 익스큐터에 적용된다")
    void applicationYml의_종료_드레인_설정이_공용_익스큐터에_적용된다() {
        contextRunner.run(context -> {
            // given
            Executor executor = context.getBean("applicationTaskExecutor", Executor.class);

            // then
            assertThat(ReflectionTestUtils.getField(executor, "waitForTasksToCompleteOnShutdown"))
                    .as("드레인 중에도 큐가 소진되어야 한다. false면 남은 작업을 즉시 버린다")
                    .isEqualTo(true);
            assertThat(ReflectionTestUtils.getField(executor, "awaitTerminationMillis"))
                    .as("익스큐터가 들고 있던 30초 드레인을 그대로 옮긴 값이다")
                    .isEqualTo(30_000L);
        });
    }
}
