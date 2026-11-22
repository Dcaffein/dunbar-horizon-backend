package com.example.DunbarHorizon.buzz.domain.model;

import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuzzCommentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    private final Long creatorId = 1L;
    private final Long authorId = 2L;
    private final Long otherId = 3L;

    private final Buzz buzz = Buzz.create(creatorId, "본문", List.of(), Set.of(authorId, otherId), NOW);
    private final BuzzThread thread = thread();

    private BuzzThread thread() {
        BuzzThread created = BuzzThread.create(buzz, authorId, true, ThreadVisibility.PUBLIC, "첫 글", List.of(), NOW);
        ReflectionTestUtils.setField(created, "id", 10L);
        return created;
    }

    private BuzzComment comment() {
        return BuzzComment.write(buzz, thread, authorId, "답글", List.of("buzz/a"), NOW);
    }

    @Test
    @DisplayName("답글은 스레드, 작성자, 본문, 이미지 키를 가진다")
    void write_Success() {
        BuzzComment comment = comment();

        assertThat(comment.getThreadId()).isEqualTo(10L);
        assertThat(comment.isAuthor(authorId)).isTrue();
        assertThat(comment.getImageKeys()).containsExactly("buzz/a");
    }

    @Test
    @DisplayName("만료된 버즈의 스레드에는 답글을 쓸 수 없다")
    void write_Fail_Expired() {
        assertThatThrownBy(() -> BuzzComment.write(buzz, thread, authorId, "답글", List.of(), NOW.plusHours(6)))
                .isInstanceOf(BuzzInvalidStateException.class);
    }

    @Test
    @DisplayName("답글이 비어 있거나 300자를 넘으면 쓸 수 없다")
    void write_Fail_InvalidText() {
        assertThatThrownBy(() -> BuzzComment.write(buzz, thread, authorId, " ", List.of(), NOW))
                .isInstanceOf(BuzzInvalidStateException.class);
        assertThatThrownBy(() -> BuzzComment.write(buzz, thread, authorId, "가".repeat(BuzzComment.TEXT_MAX_LENGTH + 1),
                List.of(), NOW))
                .isInstanceOf(BuzzInvalidStateException.class);
    }

    @Test
    @DisplayName("작성자는 답글을 수정할 수 있다")
    void update_ByAuthor() {
        BuzzComment comment = comment();

        comment.update(authorId, "수정", null);

        assertThat(comment.getText()).isEqualTo("수정");
        assertThat(comment.getImageKeys()).isEmpty();
    }

    @Test
    @DisplayName("게시자도 답글을 수정할 수 없다")
    void update_ByCreator_Denied() {
        assertThatThrownBy(() -> comment().update(creatorId, "수정", List.of()))
                .isInstanceOf(BuzzAccessDeniedException.class);
    }

    @Test
    @DisplayName("작성자와 게시자는 답글을 삭제할 수 있고 다른 사람은 거절된다")
    void validateDeletion() {
        BuzzComment comment = comment();

        comment.validateDeletion(authorId, buzz);
        comment.validateDeletion(creatorId, buzz);
        assertThatThrownBy(() -> comment.validateDeletion(otherId, buzz))
                .isInstanceOf(BuzzAccessDeniedException.class);
    }
}
