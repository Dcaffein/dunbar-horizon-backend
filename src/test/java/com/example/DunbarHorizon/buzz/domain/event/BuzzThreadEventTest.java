package com.example.DunbarHorizon.buzz.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuzzThreadEventTest {

    private final Long creatorId = 1L;
    private final Long starterId = 2L;
    private final Long friendId = 3L;

    @Test
    @DisplayName("스레드를 열면 게시자에게 알리고 시작자가 게시자에게 말을 건 것으로 본다")
    void threadOpened() {
        BuzzThreadOpenedEvent event = new BuzzThreadOpenedEvent("buzz-1", 10L, creatorId, starterId);

        assertThat(event.notificationReceiverId()).isEqualTo(creatorId);
        assertThat(event.interactionTargetId()).isEqualTo(creatorId);
    }

    @Test
    @DisplayName("다른 멤버가 답글을 쓰면 게시자와 시작자에게 알리고 시작자에게 말을 건 것으로 본다")
    void commentedByOtherMember() {
        BuzzCommentedEvent event = new BuzzCommentedEvent("buzz-1", 10L, creatorId, starterId, friendId);

        assertThat(event.notificationReceiverIds()).containsExactly(creatorId, starterId);
        assertThat(event.interactionTargetId()).isEqualTo(starterId);
    }

    @Test
    @DisplayName("시작자가 답글을 쓰면 게시자에게만 알리고 게시자에게 말을 건 것으로 본다")
    void commentedByStarter() {
        BuzzCommentedEvent event = new BuzzCommentedEvent("buzz-1", 10L, creatorId, starterId, starterId);

        assertThat(event.notificationReceiverIds()).containsExactly(creatorId);
        assertThat(event.interactionTargetId()).isEqualTo(creatorId);
    }

    @Test
    @DisplayName("게시자가 답글을 쓰면 시작자에게만 알리고 시작자에게 말을 건 것으로 본다")
    void commentedByCreator() {
        BuzzCommentedEvent event = new BuzzCommentedEvent("buzz-1", 10L, creatorId, starterId, creatorId);

        assertThat(event.notificationReceiverIds()).containsExactly(starterId);
        assertThat(event.interactionTargetId()).isEqualTo(starterId);
    }
}
