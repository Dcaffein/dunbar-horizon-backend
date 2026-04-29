package com.example.DunbarHorizon.trace;

import com.example.DunbarHorizon.support.JpaRepositoryTest;
import com.example.DunbarHorizon.trace.adapter.out.persistence.TraceRepositoryAdapter;
import com.example.DunbarHorizon.trace.domain.model.Trace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaRepositoryTest
@Import(TraceRepositoryAdapter.class)
class TraceRepositoryAdapterTest {

    @Autowired
    private TraceRepositoryAdapter traceRepositoryAdapter;

    @Autowired
    private TestEntityManager entityManager;

    private final Long user1 = 1L;
    private final Long user2 = 2L;

    @Test
    @DisplayName("Trace를 저장하면 무조건 작은 ID가 userA로 저장되고, 정확히 조회된다")
    void saveAndFind_SortsIds_Success() {
        // given: 큰 ID(2L)가 방문자, 작은 ID(1L)가 타겟으로 역방향 생성
        Trace trace = new Trace(user2, user1);

        // when
        traceRepositoryAdapter.save(trace);
        entityManager.flush();
        entityManager.clear(); // 1차 캐시를 비우고 DB에서 확실히 다시 조회

        // then: 조회할 때도 작은 값, 큰 값 순서로 조회
        Optional<Trace> foundTrace = traceRepositoryAdapter.findByUserAIdAndUserBId(user1, user2);

        assertThat(foundTrace).isPresent();
        // 식별자가 자동으로 정렬되어 저장되었는지 확인
        assertThat(foundTrace.get().getUserAId()).isEqualTo(user1);
        assertThat(foundTrace.get().getUserBId()).isEqualTo(user2);

        // 2번 유저(B)가 방문한 기록이 정확히 반영되었는지 확인
        assertThat(foundTrace.get().getUserACount()).isEqualTo(0);
        assertThat(foundTrace.get().getUserBCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 역방향인 방문 기록을 두 번 생성해 저장하면 DB 복합 유니크 제약조건에 의해 실패한다")
    void uniqueConstraint_Violation_With_ReversedIds() {
        // given: A가 B를 방문한 기록 생성 및 DB 반영
        Trace firstTrace = new Trace(user1, user2);
        traceRepositoryAdapter.save(firstTrace);
        entityManager.flush();

        // when & then: B가 A를 방문한 '새로운' 기록을 생성하려 시도
        // (도메인 생성자에서 A, B가 다시 1, 2로 정렬되므로 기존 로우와 복합키가 충돌해야 함)
        Trace secondTrace = new Trace(user2, user1);

        assertThatThrownBy(() -> {
            traceRepositoryAdapter.save(secondTrace);
            entityManager.flush(); // 즉시 쿼리를 날려 제약 조건 위반을 유도
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 방문 기록을 조회하면 빈 Optional을 반환한다")
    void findByUserAIdAndUserBId_Empty() {
        // when
        Optional<Trace> result = traceRepositoryAdapter.findByUserAIdAndUserBId(999L, 888L);

        // then
        assertThat(result).isEmpty();
    }
}