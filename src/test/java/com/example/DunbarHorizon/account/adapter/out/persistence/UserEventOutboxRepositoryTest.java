package com.example.DunbarHorizon.account.adapter.out.persistence;

import com.example.DunbarHorizon.account.adapter.out.persistence.jpa.UserEventOutboxJpaRepository;
import com.example.DunbarHorizon.account.domain.outbox.UserEventOutbox;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxEventType;
import com.example.DunbarHorizon.account.domain.outbox.UserOutboxStatus;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JpaRepositoryTest
class UserEventOutboxRepositoryTest {

    @Autowired
    private UserEventOutboxJpaRepository jpaRepository;

    @Test
    void PENDING_상태의_레코드를_저장하고_조회할_수_있다() {
        // given
        UserEventOutbox outbox = UserEventOutbox.pending(1L, UserOutboxEventType.ACTIVATE, "{\"userId\":1}");
        jpaRepository.save(outbox);

        // when
        List<UserEventOutbox> result = jpaRepository.findByStatusAndCreatedAtBefore(
                UserOutboxStatus.PENDING, LocalDateTime.now().plusSeconds(1)
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAggregateId()).isEqualTo(1L);
        assertThat(result.get(0).getStatus()).isEqualTo(UserOutboxStatus.PENDING);
    }

    @Test
    void COMPLETED로_변경된_레코드는_PENDING_조회에서_제외된다() {
        // given
        UserEventOutbox outbox = UserEventOutbox.pending(2L, UserOutboxEventType.ACTIVATE, "{}");
        outbox.markCompleted();
        jpaRepository.save(outbox);

        // when
        List<UserEventOutbox> result = jpaRepository.findByStatusAndCreatedAtBefore(
                UserOutboxStatus.PENDING, LocalDateTime.now().plusSeconds(1)
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void createdAt_임계값보다_최신_레코드는_조회되지_않는다() {
        // given
        UserEventOutbox outbox = UserEventOutbox.pending(3L, UserOutboxEventType.DEACTIVATE, "{}");
        jpaRepository.save(outbox);

        // when: 과거 시각 기준으로 조회
        List<UserEventOutbox> result = jpaRepository.findByStatusAndCreatedAtBefore(
                UserOutboxStatus.PENDING, LocalDateTime.now().minusMinutes(10)
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void markCompleted_호출_시_상태가_COMPLETED로_변경되고_processedAt이_설정된다() {
        // given
        UserEventOutbox outbox = UserEventOutbox.pending(4L, UserOutboxEventType.ACTIVATE, "{}");
        jpaRepository.save(outbox);

        // when
        outbox.markCompleted();
        jpaRepository.save(outbox);

        // then
        UserEventOutbox found = jpaRepository.findById(outbox.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(UserOutboxStatus.COMPLETED);
        assertThat(found.getProcessedAt()).isNotNull();
    }
}
