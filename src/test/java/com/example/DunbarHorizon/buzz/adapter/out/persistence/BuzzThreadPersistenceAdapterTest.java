package com.example.DunbarHorizon.buzz.adapter.out.persistence;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JpaRepositoryTest
@Import(BuzzThreadRepositoryAdapter.class)
class BuzzThreadPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    @Autowired private BuzzThreadRepositoryAdapter buzzThreadRepositoryAdapter;
    @Autowired private EntityManager entityManager;

    private BuzzThread create(String buzzId, Long starterId, ThreadVisibility visibility, LocalDateTime createdAt) {
        return buzzThreadRepositoryAdapter.save(
                BuzzThread.create(buzzId, starterId, visibility, "첫 글", List.of("buzz/a"), createdAt));
    }

    @Test
    @DisplayName("한 멤버는 한 버즈에 공개 스레드와 비공개 스레드를 하나씩 생성할 수 있다")
    void save_PublicAndPrivate_BothSaved() {
        // when
        create("buzz-1", 2L, ThreadVisibility.PUBLIC, NOW);
        create("buzz-1", 2L, ThreadVisibility.PRIVATE, NOW);

        // then
        assertThat(buzzThreadRepositoryAdapter.findAllByBuzzId("buzz-1")).hasSize(2);
    }

    @Test
    @DisplayName("같은 멤버가 같은 공개 범위의 스레드를 다시 생성하면 중복 예외로 바뀐다")
    void save_SameVisibilityTwice_Throws() {
        // given
        create("buzz-1", 2L, ThreadVisibility.PUBLIC, NOW);

        // when & then
        assertThatThrownBy(() -> create("buzz-1", 2L, ThreadVisibility.PUBLIC, NOW))
                .isInstanceOf(BuzzThreadAlreadyExistsException.class);
    }

    @Test
    @DisplayName("버즈의 스레드는 생성한 순서대로 조회되고 다른 버즈의 스레드는 섞이지 않는다")
    void findAllByBuzzId_OrderedByCreatedAt() {
        // given
        BuzzThread later = create("buzz-1", 3L, ThreadVisibility.PUBLIC, NOW.plusMinutes(5));
        BuzzThread earlier = create("buzz-1", 2L, ThreadVisibility.PUBLIC, NOW);
        create("buzz-2", 2L, ThreadVisibility.PUBLIC, NOW);
        entityManager.clear();

        // when
        List<BuzzThread> threads = buzzThreadRepositoryAdapter.findAllByBuzzId("buzz-1");

        // then
        assertThat(threads).extracting(BuzzThread::getId).containsExactly(earlier.getId(), later.getId());
    }

    @Test
    @DisplayName("버즈 단위로 스레드를 지우면 다른 버즈의 스레드는 남는다")
    void deleteAllByBuzzId_KeepsOtherBuzz() {
        // given
        create("buzz-1", 2L, ThreadVisibility.PUBLIC, NOW);
        create("buzz-2", 2L, ThreadVisibility.PUBLIC, NOW);

        // when
        buzzThreadRepositoryAdapter.deleteAllByBuzzId("buzz-1");

        // then
        assertThat(buzzThreadRepositoryAdapter.findAllByBuzzId("buzz-1")).isEmpty();
        assertThat(buzzThreadRepositoryAdapter.findAllByBuzzId("buzz-2")).hasSize(1);
    }
}
