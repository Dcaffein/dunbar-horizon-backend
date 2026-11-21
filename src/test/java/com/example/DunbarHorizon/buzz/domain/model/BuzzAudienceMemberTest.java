package com.example.DunbarHorizon.buzz.domain.model;

import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuzzAudienceMemberTest {

    @Test
    @DisplayName("공개 대상 멤버는 미읽음으로 시작한다")
    void create_StartsUnread() {
        BuzzAudienceMember member = BuzzAudienceMember.create("buzz-1", 2L);

        assertThat(member.isUnread()).isTrue();
        assertThat(member.isNew()).isTrue();
    }

    @Test
    @DisplayName("읽음 처리를 반복해도 읽음 상태로 남는다")
    void markAsRead_Idempotent() {
        // given
        BuzzAudienceMember member = BuzzAudienceMember.create("buzz-1", 2L);

        // when
        member.markAsRead();
        member.markAsRead();

        // then
        assertThat(member.isUnread()).isFalse();
    }

    @Test
    @DisplayName("식별자는 버즈 ID와 사용자 ID의 조합이다")
    void getId_BuzzIdAndUserId() {
        BuzzAudienceMember member = BuzzAudienceMember.create("buzz-1", 2L);

        assertThat(member.getId()).isEqualTo(new BuzzAudienceMember.Key("buzz-1", 2L));
    }
}
