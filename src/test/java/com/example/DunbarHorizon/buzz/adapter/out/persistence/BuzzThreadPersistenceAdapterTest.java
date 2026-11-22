package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyExistsException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaRepositoryTest
@Import(BuzzThreadRepositoryAdapter.class)
class BuzzThreadPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    @Autowired private BuzzThreadRepositoryAdapter buzzThreadRepositoryAdapter;
    @Autowired private EntityManager entityManager;

    private final Buzz firstBuzz = withId(Buzz.create(1L, "본문", List.of(), Set.of(2L, 3L), NOW), 900_001L);
    private final Buzz secondBuzz = withId(Buzz.create(1L, "본문", List.of(), Set.of(2L, 3L), NOW), 900_002L);

    private BuzzThread create(Buzz buzz, Long starterId, ThreadVisibility visibility, LocalDateTime createdAt) {
        return buzzThreadRepositoryAdapter.save(
                BuzzThread.create(buzz, starterId, true, visibility, "첫 글", List.of("buzz/a"), createdAt));
    }

    @Test
    @DisplayName("한 멤버는 한 버즈에 공개 스레드와 비공개 스레드를 하나씩 생성할 수 있다")
    void save_PublicAndPrivate_BothSaved() {
        // when
        create(firstBuzz, 2L, ThreadVisibility.PUBLIC, NOW);
        create(firstBuzz, 2L, ThreadVisibility.PRIVATE, NOW);

        // then
        assertThat(buzzThreadRepositoryAdapter.findAllByBuzzId(firstBuzz.getId())).hasSize(2);
    }

    @Test
    @DisplayName("같은 멤버가 같은 공개 범위의 스레드를 다시 생성하면 중복 예외로 바뀐다")
    void save_SameVisibilityTwice_Throws() {
        // given
        create(firstBuzz, 2L, ThreadVisibility.PUBLIC, NOW);

        // when & then
        assertThatThrownBy(() -> create(firstBuzz, 2L, ThreadVisibility.PUBLIC, NOW))
                .isInstanceOf(BuzzThreadAlreadyExistsException.class);
    }

    @Test
    @DisplayName("버즈의 스레드는 생성한 순서대로 조회되고 다른 버즈의 스레드는 섞이지 않는다")
    void findAllByBuzzId_OrderedByCreatedAt() {
        // given
        BuzzThread later = create(firstBuzz, 3L, ThreadVisibility.PUBLIC, NOW.plusMinutes(5));
        BuzzThread earlier = create(firstBuzz, 2L, ThreadVisibility.PUBLIC, NOW);
        create(secondBuzz, 2L, ThreadVisibility.PUBLIC, NOW);
        entityManager.clear();

        // when
        List<BuzzThread> threads = buzzThreadRepositoryAdapter.findAllByBuzzId(firstBuzz.getId());

        // then
        assertThat(threads).extracting(BuzzThread::getId).containsExactly(earlier.getId(), later.getId());
    }

    @Test
    @DisplayName("버즈들의 스레드 ID를 조회하고 버즈 단위로 지우면 다른 버즈의 스레드는 남는다")
    void deleteAllByBuzzIds_KeepsOtherBuzz() {
        // given
        BuzzThread target = create(firstBuzz, 2L, ThreadVisibility.PUBLIC, NOW);
        create(secondBuzz, 2L, ThreadVisibility.PUBLIC, NOW);

        // when
        List<Long> threadIds = buzzThreadRepositoryAdapter.findIdsByBuzzIds(List.of(firstBuzz.getId()));
        buzzThreadRepositoryAdapter.deleteAllByBuzzIds(List.of(firstBuzz.getId()));

        // then
        assertThat(threadIds).containsExactly(target.getId());

        // then
        assertThat(buzzThreadRepositoryAdapter.findAllByBuzzId(firstBuzz.getId())).isEmpty();
        assertThat(buzzThreadRepositoryAdapter.findAllByBuzzId(secondBuzz.getId())).hasSize(1);
    }

    private static Buzz withId(Buzz buzz, long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(buzz, "id", id);
        return buzz;
    }
}
