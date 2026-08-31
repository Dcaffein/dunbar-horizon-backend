package com.example.DunbarHorizon.support;

import com.example.DunbarHorizon.account.adapter.out.persistence.UserEventOutboxRepositoryAdapter;
import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserEventOutboxJpaRepository;
import com.example.DunbarHorizon.account.application.eventListener.UserOutboxEventListener;
import com.example.DunbarHorizon.account.application.service.UserOutboxRetryService;
import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.repository.UserEventOutboxRepository;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.SocialUserRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.application.eventListener.SocialUserEventListener;
import com.example.DunbarHorizon.social.application.service.SocialUserSyncCommandService;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 전체 앱 스캔 없이 실제 DB, 저장소, 이벤트 리스너와 트랜잭션/비동기 프록시만 구성한다. */
@TestConfiguration(proxyBeanMethods = false)
@Import(TestContainerConfig.class)
@ImportAutoConfiguration({
        ServiceConnectionAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        Neo4jAutoConfiguration.class,
        Neo4jDataAutoConfiguration.class
})
@EntityScan(basePackages = {
        "com.example.DunbarHorizon.account.domain",
        "com.example.DunbarHorizon.social.domain"
})
@EnableJpaRepositories(basePackageClasses = UserEventOutboxJpaRepository.class)
@EnableNeo4jRepositories(
        basePackageClasses = SocialUserNeo4jRepository.class,
        transactionManagerRef = "neo4jTransactionManager"
)
@EnableJpaAuditing
@EnableNeo4jAuditing
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAsync(proxyTargetClass = true)
public class UserSyncIntegrationTestConfig {

    @Bean
    @Primary
    PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    Neo4jTransactionManager neo4jTransactionManager(Driver driver, DatabaseSelectionProvider databases) {
        return new Neo4jTransactionManager(driver, databases);
    }

    @Bean
    UserSyncFaultProbe userSyncFaultProbe(ApplicationContext applicationContext) {
        return new UserSyncFaultProbe(applicationContext);
    }

    @Bean(name = "taskExecutor")
    ThreadPoolTaskExecutor taskExecutor(UserSyncFaultProbe faults) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("user-sync-integration-");
        executor.setTaskDecorator(faults::trackWorker);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(UserSyncFaultProbe.TIMEOUT_SECONDS);
        return executor;
    }

    @Bean
    UserEventOutboxRepository outboxRepository(UserEventOutboxJpaRepository repository, UserSyncFaultProbe faults) {
        return new FixtureScopedOutboxRepository(new UserEventOutboxRepositoryAdapter(repository), faults);
    }

    @Bean
    SocialUserRepository socialUserRepository(SocialUserNeo4jRepository repository, UserSyncFaultProbe faults) {
        return new CommitProbedSocialUserRepository(new SocialUserRepositoryAdapter(repository), faults);
    }

    @Bean
    UserOutboxEventListener userOutboxEventListener(UserEventOutboxRepository repository, UserSyncFaultProbe faults) {
        return new UserOutboxEventListener(repository, faults, new ObjectMapper());
    }

    @Bean
    UserOutboxRetryService userOutboxRetryService(UserEventOutboxRepository repository, UserSyncFaultProbe faults) {
        return new UserOutboxRetryService(repository, faults, new ObjectMapper());
    }

    @Bean
    SocialUserSyncCommandService socialUserSyncCommandService(SocialUserRepository repository) {
        return new SocialUserSyncCommandService(repository);
    }

    @Bean
    SocialUserEventListener socialUserEventListener(SocialUserSyncCommandService service, UserSyncFaultProbe faults) {
        return new SocialUserEventListener(service, faults);
    }

    /** 재사용 DB에 남은 다른 테스트의 PENDING을 이번 재시도가 변경하지 않도록 한정한다. */
    private record FixtureScopedOutboxRepository(
            UserEventOutboxRepository delegate,
            UserSyncFaultProbe faults
    ) implements UserEventOutboxRepository {
        @Override
        public UserEventOutbox save(UserEventOutbox outbox) {
            return delegate.save(outbox);
        }

        @Override
        public Optional<UserEventOutbox> findById(String id) {
            return delegate.findById(id);
        }

        @Override
        public List<UserEventOutbox> findPendingOlderThan(LocalDateTime threshold) {
            return delegate.findPendingOlderThan(threshold).stream()
                    .filter(outbox -> faults.ownsOutbox(outbox.getId()))
                    .toList();
        }

        @Override
        public void deleteProcessedOlderThan(LocalDateTime threshold) {
            throw new UnsupportedOperationException("이 통합 컨텍스트에서는 전역 Outbox 정리를 실행하지 않는다.");
        }
    }

    private record CommitProbedSocialUserRepository(
            SocialUserRepository delegate,
            UserSyncFaultProbe faults
    ) implements SocialUserRepository {
        @Override
        public Optional<SocialUser> findById(Long id) {
            return delegate.findById(id);
        }

        @Override
        public Set<UserReference> findAllUserReferencesById(Collection<Long> ids) {
            return delegate.findAllUserReferencesById(ids);
        }

        @Override
        public SocialUser save(SocialUser user) {
            SocialUser saved = delegate.save(user);
            faults.afterGraphSave(user.getId());
            return saved;
        }

        @Override
        public Set<SocialUser> saveAll(List<SocialUser> users) {
            Set<SocialUser> saved = delegate.saveAll(users);
            users.forEach(user -> faults.afterGraphSave(user.getId()));
            return saved;
        }
    }
}
