package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.WriteBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCommentedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzCommentNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzCommentRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuzzCommentServiceTest {

    @Mock private BuzzRepository buzzRepository;
    @Mock private BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    @Mock private BuzzThreadRepository buzzThreadRepository;
    @Mock private BuzzCommentRepository buzzCommentRepository;
    @Mock private BuzzSocialPort buzzSocialPort;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private BuzzCommentService buzzCommentService;

    private final Long creatorId = 1L;
    private final Long starterId = 2L;
    private final Long friendMemberId = 3L;
    private final Long otherMemberId = 4L;
    private Buzz buzz;
    private BuzzThread thread;

    @BeforeEach
    void setUp() {
        buzzCommentService = new BuzzCommentService(buzzRepository, buzzThreadRepository, buzzCommentRepository,
                new BuzzThreadAccess(buzzRepository, buzzAudienceMemberRepository, buzzThreadRepository, buzzSocialPort),
                applicationEventPublisher);
        buzz = Buzz.create(creatorId, "본문", List.of(),
                Set.of(starterId, friendMemberId, otherMemberId), LocalDateTime.now());
        thread = BuzzThread.open(buzz.getId(), starterId, ThreadVisibility.PUBLIC, "첫 글", List.of(), LocalDateTime.now());
        ReflectionTestUtils.setField(thread, "id", 10L);
    }

    @Nested
    @DisplayName("답글 쓰기")
    class WriteComment {

        @BeforeEach
        void setUp() {
            given(buzzRepository.findByIdForShare(buzz.getId())).willReturn(Optional.of(buzz));
        }

        @Test
        @DisplayName("시작자의 친구인 멤버가 공개 스레드에 답글을 쓰면 저장되고 그 멤버는 읽음이 된다")
        void writeComment_ByFriendMember_SavesAndMarksRead() {
            // given
            BuzzAudienceMember member = BuzzAudienceMember.create(buzz.getId(), friendMemberId);
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), friendMemberId))
                    .willReturn(Optional.of(member));
            given(buzzThreadRepository.findByIdForShare(10L)).willReturn(Optional.of(thread));
            given(buzzSocialPort.getFriendIdsIn(friendMemberId, List.of(starterId))).willReturn(Set.of(starterId));
            given(buzzCommentRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            // when
            buzzCommentService.writeComment(command(friendMemberId));

            // then
            ArgumentCaptor<BuzzComment> commentCaptor = ArgumentCaptor.forClass(BuzzComment.class);
            verify(buzzCommentRepository).save(commentCaptor.capture());
            assertThat(commentCaptor.getValue().getThreadId()).isEqualTo(10L);
            assertThat(commentCaptor.getValue().getAuthorId()).isEqualTo(friendMemberId);
            assertThat(member.isUnread()).isFalse();
            verify(applicationEventPublisher).publishEvent(
                    new BuzzCommentedEvent(buzz.getId(), 10L, creatorId, starterId, friendMemberId));
        }

        @Test
        @DisplayName("시작자의 친구가 아닌 멤버는 공개 스레드에 답글을 쓸 수 없다")
        void writeComment_ByNonFriend_Denied() {
            // given
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), otherMemberId))
                    .willReturn(Optional.of(BuzzAudienceMember.create(buzz.getId(), otherMemberId)));
            given(buzzThreadRepository.findByIdForShare(10L)).willReturn(Optional.of(thread));
            given(buzzSocialPort.getFriendIdsIn(otherMemberId, List.of(starterId))).willReturn(Set.of());

            // when & then
            assertThatThrownBy(() -> buzzCommentService.writeComment(command(otherMemberId)))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzCommentRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("게시자는 친구 관계 확인 없이 답글을 쓴다")
        void writeComment_ByCreator() {
            // given
            given(buzzThreadRepository.findByIdForShare(10L)).willReturn(Optional.of(thread));
            given(buzzCommentRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            // when
            buzzCommentService.writeComment(command(creatorId));

            // then
            verify(buzzCommentRepository).save(any());
            verify(buzzSocialPort, never()).getFriendIdsIn(any(), any());
        }

        private WriteBuzzCommentCommand command(Long authorId) {
            return new WriteBuzzCommentCommand(authorId, buzz.getId(), 10L, "답글", List.of());
        }
    }

    @Test
    @DisplayName("만료된 버즈의 스레드에는 답글을 쓸 수 없다")
    void writeComment_Expired_Rejected() {
        // given
        Buzz expired = Buzz.create(creatorId, "본문", List.of(), Set.of(starterId), LocalDateTime.now().minusHours(7));
        given(buzzRepository.findByIdForShare(expired.getId())).willReturn(Optional.of(expired));

        // when & then
        assertThatThrownBy(() -> buzzCommentService.writeComment(
                new WriteBuzzCommentCommand(starterId, expired.getId(), 10L, "답글", List.of())))
                .isInstanceOf(BuzzInvalidStateException.class);
    }

    @Nested
    @DisplayName("답글 수정과 삭제")
    class Modification {

        private BuzzComment comment;

        @BeforeEach
        void setUp() {
            comment = BuzzComment.write(10L, friendMemberId, "답글", List.of(), LocalDateTime.now());
            ReflectionTestUtils.setField(comment, "id", 100L);
            given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread));
        }

        @Test
        @DisplayName("작성자가 답글을 수정하면 본문이 바뀐다")
        void updateComment_ByAuthor() {
            // given
            given(buzzCommentRepository.findById(100L)).willReturn(Optional.of(comment));

            // when
            buzzCommentService.updateComment(new UpdateBuzzCommentCommand(
                    friendMemberId, buzz.getId(), 10L, 100L, "수정", List.of()));

            // then
            assertThat(comment.getText()).isEqualTo("수정");
        }

        @Test
        @DisplayName("게시자는 다른 사람의 답글을 삭제할 수 있다")
        void deleteComment_ByCreator() {
            // given
            given(buzzCommentRepository.findById(100L)).willReturn(Optional.of(comment));

            // when
            buzzCommentService.deleteComment(creatorId, buzz.getId(), 10L, 100L);

            // then
            verify(buzzCommentRepository).deleteById(100L);
        }

        @Test
        @DisplayName("스레드 시작자라도 다른 사람의 답글은 삭제할 수 없다")
        void deleteComment_ByStarter_Denied() {
            // given
            given(buzzCommentRepository.findById(100L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> buzzCommentService.deleteComment(starterId, buzz.getId(), 10L, 100L))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("다른 스레드의 답글 ID로 수정하면 찾을 수 없다")
        void updateComment_OtherThreadComment_NotFound() {
            // given
            BuzzComment otherThreadComment = BuzzComment.write(99L, friendMemberId, "답글", List.of(), LocalDateTime.now());
            given(buzzCommentRepository.findById(100L)).willReturn(Optional.of(otherThreadComment));

            // when & then
            assertThatThrownBy(() -> buzzCommentService.updateComment(new UpdateBuzzCommentCommand(
                    friendMemberId, buzz.getId(), 10L, 100L, "수정", List.of())))
                    .isInstanceOf(BuzzCommentNotFoundException.class);
        }
    }
}
