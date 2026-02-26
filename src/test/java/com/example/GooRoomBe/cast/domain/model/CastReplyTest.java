package com.example.GooRoomBe.cast.domain.model;

import com.example.GooRoomBe.cast.domain.exception.CastInvalidStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CastReplyTest {

    @Test
    @DisplayName("답장 생성 시 모든 필드가 정상 주입된다")
    void of_Success() {
        CastReply response = CastReply.of(
                "res-1", 1L, "닉네임", "profile.jpg", "내용", null, true);

        assertThat(response.getReplierNickname()).isEqualTo("닉네임");
        assertThat(response.getText()).isEqualTo("내용");
    }

    @Test
    @DisplayName("답장 내용을 비워서 수정하면 예외가 발생한다")
    void update_Fail_EmptyText() {
        CastReply response = CastReply.of("res-1", 1L, "닉", "p.jpg", "Old", null, true);

        assertThatThrownBy(() -> response.update("", null))
                .isInstanceOf(CastInvalidStateException.class);
    }
}