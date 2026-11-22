package com.example.DunbarHorizon.buzz.domain.model;

import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuzzThreadTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    private final Long creatorId = 1L;
    private final Long starterId = 2L;
    private final Long otherMemberId = 3L;

    private BuzzThread thread(ThreadVisibility visibility) {
        return BuzzThread.create("buzz-1", starterId, visibility, "첫 글", List.of("buzz/a"), NOW);
    }

    @Nested
    @DisplayName("스레드 생성")
    class CreateTest {

        @Test
        @DisplayName("첫 글과 공개 범위, 이미지 키를 가진 스레드를 생성한다")
        void create_Success() {
            BuzzThread thread = thread(ThreadVisibility.PRIVATE);

            assertThat(thread.getStarterId()).isEqualTo(starterId);
            assertThat(thread.getVisibility()).isEqualTo(ThreadVisibility.PRIVATE);
            assertThat(thread.getText()).isEqualTo("첫 글");
            assertThat(thread.getImageKeys()).containsExactly("buzz/a");
            assertThat(thread.getCreatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("첫 글이 비어 있으면 생성할 수 없다")
        void create_Fail_BlankText() {
            assertThatThrownBy(() -> BuzzThread.create("buzz-1", starterId, ThreadVisibility.PUBLIC, " ", List.of(), NOW))
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("첫 글이 300자를 넘으면 생성할 수 없다")
        void create_Fail_TooLongText() {
            String tooLong = "가".repeat(BuzzThread.TEXT_MAX_LENGTH + 1);

            assertThatThrownBy(() -> BuzzThread.create("buzz-1", starterId, ThreadVisibility.PUBLIC, tooLong, List.of(), NOW))
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("공개 범위가 없으면 생성할 수 없다")
        void create_Fail_NoVisibility() {
            assertThatThrownBy(() -> BuzzThread.create("buzz-1", starterId, null, "첫 글", List.of(), NOW))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("첫 글 수정과 스레드 삭제")
    class ModificationTest {

        @Test
        @DisplayName("시작자는 첫 글을 수정할 수 있다")
        void update_ByStarter() {
            BuzzThread thread = thread(ThreadVisibility.PUBLIC);

            thread.update(starterId, "수정한 글", List.of());

            assertThat(thread.getText()).isEqualTo("수정한 글");
            assertThat(thread.getImageKeys()).isEmpty();
        }

        @Test
        @DisplayName("게시자도 첫 글을 수정할 수 없다")
        void update_ByCreator_Denied() {
            assertThatThrownBy(() -> thread(ThreadVisibility.PUBLIC).update(creatorId, "수정", List.of()))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("수정한 첫 글도 길이 규칙을 지켜야 한다")
        void update_InvalidText_Rejected() {
            assertThatThrownBy(() -> thread(ThreadVisibility.PUBLIC).update(starterId, " ", List.of()))
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        @Test
        @DisplayName("시작자와 게시자는 스레드를 삭제할 수 있고 다른 멤버는 거절된다")
        void validateDeletion() {
            BuzzThread thread = thread(ThreadVisibility.PUBLIC);

            thread.validateDeletion(starterId, creatorId);
            thread.validateDeletion(creatorId, creatorId);
            assertThatThrownBy(() -> thread.validateDeletion(otherMemberId, creatorId))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("공개 스레드 접근")
    class PublicAccessTest {

        private final BuzzThread thread = thread(ThreadVisibility.PUBLIC);

        @Test
        @DisplayName("시작자와 게시자는 친구 관계와 관계없이 접근한다")
        void starterAndCreator_Access() {
            assertThat(thread.isAccessibleBy(starterId, creatorId, false)).isTrue();
            assertThat(thread.isAccessibleBy(creatorId, creatorId, false)).isTrue();
        }

        @Test
        @DisplayName("시작자의 친구인 다른 멤버는 접근한다")
        void friendOfStarter_Access() {
            assertThat(thread.isAccessibleBy(otherMemberId, creatorId, true)).isTrue();
        }

        @Test
        @DisplayName("시작자의 친구가 아닌 다른 멤버는 접근이 거절된다")
        void nonFriend_Denied() {
            assertThat(thread.isAccessibleBy(otherMemberId, creatorId, false)).isFalse();
            assertThatThrownBy(() -> thread.validateAccess(otherMemberId, creatorId, false))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("시작자·게시자가 아닌 사람만 친구 관계 확인이 필요하다")
        void needsFriendship_OnlyForOthers() {
            assertThat(thread.needsFriendshipToAccess(otherMemberId, creatorId)).isTrue();
            assertThat(thread.needsFriendshipToAccess(starterId, creatorId)).isFalse();
            assertThat(thread.needsFriendshipToAccess(creatorId, creatorId)).isFalse();
        }
    }

    @Nested
    @DisplayName("비공개 스레드 접근")
    class PrivateAccessTest {

        private final BuzzThread thread = thread(ThreadVisibility.PRIVATE);

        @Test
        @DisplayName("시작자와 게시자만 접근한다")
        void onlyStarterAndCreator() {
            assertThat(thread.isAccessibleBy(starterId, creatorId, false)).isTrue();
            assertThat(thread.isAccessibleBy(creatorId, creatorId, false)).isTrue();
        }

        @Test
        @DisplayName("시작자의 친구인 멤버도 접근이 거절된다")
        void friendOfStarter_Denied() {
            assertThat(thread.isAccessibleBy(otherMemberId, creatorId, true)).isFalse();
        }

        @Test
        @DisplayName("친구 관계 확인이 필요하지 않다")
        void needsFriendship_Never() {
            assertThat(thread.needsFriendshipToAccess(otherMemberId, creatorId)).isFalse();
        }
    }
}
