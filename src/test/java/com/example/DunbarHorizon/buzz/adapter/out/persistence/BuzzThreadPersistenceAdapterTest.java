package com.example.DunbarHorizon.buzz.adapter.out.persistence;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyExistsException;
import com.example.DunbarHorizon.support.JpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

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
    @Autowired private JdbcTemplate jdbcTemplate;

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
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM buzz_threads WHERE buzz_id = ?", Long.class, firstBuzz.getId())).isEqualTo(2);
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

    private static Buzz withId(Buzz buzz, long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(buzz, "id", id);
        return buzz;
    }
}
