package com.example.GooRoomBe.trace;

import com.example.GooRoomBe.support.JpaRepositoryTest;
import com.example.GooRoomBe.trace.adapter.out.persistence.TraceRepositoryAdapter;
import com.example.GooRoomBe.trace.domain.model.Trace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@JpaRepositoryTest
@Import(TraceRepositoryAdapter.class)
class TraceRepositoryAdapterTest {

    @Autowired
    private TraceRepositoryAdapter traceRepositoryAdapter;

    @Autowired
    private TestEntityManager entityManager;

    private final Long visitorId = 1L;
    private final Long targetId = 2L;

    @Test
    @DisplayName("Trace를 저장하고 포트 메서드명으로 정확히 조회한다")
    void saveAndFind_Success() {
        // given
        Trace trace = new Trace(visitorId, targetId);

        // when
        traceRepositoryAdapter.save(trace);

        // then
        Optional<Trace> found = traceRepositoryAdapter.findByVisitorAndTarget(visitorId, targetId);

        assertThat(found).isPresent();
        assertThat(found.get().getVisitorId()).isEqualTo(visitorId);
        assertThat(found.get().getTargetId()).isEqualTo(targetId);
        assertThat(found.get().getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일한 방문자와 피방문자 쌍을 저장하면 DB 유니크 제약 조건에 의해 에러가 발생한다")
    void uniqueConstraint_Violation() {
        // given
        Trace firstTrace = new Trace(visitorId, targetId);
        traceRepositoryAdapter.save(firstTrace);
        entityManager.flush(); // DB에 즉시 반영하여 제약 조건 활성화

        // when & then
        Trace secondTrace = new Trace(visitorId, targetId);

        assertThatThrownBy(() -> {
            traceRepositoryAdapter.save(secondTrace);
            entityManager.flush(); // 여기서 유니크 제약 위반 발생
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 방문 기록을 조회하면 빈 Optional을 반환한다")
    void findByVisitorAndTarget_Empty() {
        // when
        Optional<Trace> result = traceRepositoryAdapter.findByVisitorAndTarget(999L, 888L);

        // then
        assertThat(result).isEmpty();
    }
}