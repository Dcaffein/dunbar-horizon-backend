package com.example.DunbarHorizon.buzz.domain.model;

import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuzzCommentTest {

    @Test
    @DisplayName("댓글 생성 시 모든 필드가 정상 주입된다")
    void of_Success() {
        BuzzComment comment = BuzzComment.of(
                "com-1", 1L, "닉네임", "profile.jpg", "내용", null, true);

        assertThat(comment.getCommenterNickname()).isEqualTo("닉네임");
        assertThat(comment.getText()).isEqualTo("내용");
    }

    @Test
    @DisplayName("댓글 내용을 비워서 수정하면 예외가 발생한다")
    void update_Fail_EmptyText() {
        BuzzComment comment = BuzzComment.of("com-1", 1L, "닉", "p.jpg", "Old", null, true);

        assertThatThrownBy(() -> comment.update("", null))
                .isInstanceOf(BuzzInvalidStateException.class);
    }
}
