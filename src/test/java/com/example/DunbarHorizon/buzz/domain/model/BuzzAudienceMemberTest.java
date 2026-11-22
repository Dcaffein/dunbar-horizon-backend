package com.example.DunbarHorizon.buzz.domain.model;

import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuzzAudienceMemberTest {

    @Test
    @DisplayName("공개 대상 멤버는 미읽음으로 시작한다")
    void create_StartsUnread() {
        BuzzAudienceMember member = BuzzAudienceMember.create(1L, 2L);

        assertThat(member.isUnread()).isTrue();
    }

    @Test
    @DisplayName("읽음 처리를 반복해도 읽음 상태로 남는다")
    void markAsRead_Idempotent() {
        // given
        BuzzAudienceMember member = BuzzAudienceMember.create(1L, 2L);

        // when
        member.markAsRead();
        member.markAsRead();

        // then
        assertThat(member.isUnread()).isFalse();
    }
}
