package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import com.example.DunbarHorizon.buzz.domain.event.BuzzThreadCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyExistsException;
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
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuzzThreadServiceTest {

    @Mock private BuzzRepository buzzRepository;
    @Mock private BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    @Mock private BuzzThreadRepository buzzThreadRepository;
    @Mock private BuzzCommentRepository buzzCommentRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private BuzzThreadService buzzThreadService;

    private final Long creatorId = 1L;
    private final Long starterId = 2L;
    private final Long friendMemberId = 3L;
    private final Long otherMemberId = 4L;
    private Buzz buzz;

    @BeforeEach
    void setUp() {
        buzzThreadService = new BuzzThreadService(buzzRepository, buzzAudienceMemberRepository, buzzThreadRepository,
                buzzCommentRepository, applicationEventPublisher);
        buzz = withId(Buzz.create(creatorId, "본문", List.of(),
                Set.of(starterId, friendMemberId, otherMemberId), LocalDateTime.now()), 1L);
    }

    @Nested
    @DisplayName("스레드 생성")
    class CreateThread {

        @Test
        @DisplayName("공개 대상 멤버가 스레드를 생성하면 저장하고 읽음 상태는 바꾸지 않는다")
        void createThread_ByMember_SavesWithoutChangingReadState() {
            // given
            BuzzAudienceMember member = BuzzAudienceMember.create(buzz.getId(), starterId);
            given(buzzRepository.findByIdForShare(buzz.getId())).willReturn(Optional.of(buzz));
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), starterId))
                    .willReturn(Optional.of(member));
            given(buzzThreadRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            // when
            buzzThreadService.createThread(command(starterId, ThreadVisibility.PRIVATE));

            // then
            ArgumentCaptor<BuzzThread> threadCaptor = ArgumentCaptor.forClass(BuzzThread.class);
            verify(buzzThreadRepository).save(threadCaptor.capture());
            assertThat(threadCaptor.getValue().getVisibility()).isEqualTo(ThreadVisibility.PRIVATE);
            assertThat(threadCaptor.getValue().getImageKeys()).containsExactly("buzz/a");
            assertThat(member.isUnread()).isTrue();
            verify(buzzAudienceMemberRepository, never()).save(any());
            verify(applicationEventPublisher).publishEvent(
                    new BuzzThreadCreatedEvent(buzz.getId(), null, creatorId, starterId));
        }

        @Test
        @DisplayName("스레드 저장이 실패하면 시작 이벤트를 발행하지 않는다")
        void createThread_SaveFails_NoEvent() {
            // given
            given(buzzRepository.findByIdForShare(buzz.getId())).willReturn(Optional.of(buzz));
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), starterId))
                    .willReturn(Optional.of(BuzzAudienceMember.create(buzz.getId(), starterId)));
            given(buzzThreadRepository.save(any()))
                    .willThrow(new BuzzThreadAlreadyExistsException(buzz.getId(), starterId, ThreadVisibility.PUBLIC));

            // when & then
            assertThatThrownBy(() -> buzzThreadService.createThread(command(starterId, ThreadVisibility.PUBLIC)))
                    .isInstanceOf(BuzzThreadAlreadyExistsException.class);
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("게시자는 스레드를 생성할 수 없다")
        void createThread_ByCreator_Denied() {
            given(buzzRepository.findByIdForShare(buzz.getId())).willReturn(Optional.of(buzz));

            assertThatThrownBy(() -> buzzThreadService.createThread(command(creatorId, ThreadVisibility.PUBLIC)))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzThreadRepository, never()).save(any());
        }

        @Test
        @DisplayName("만료된 버즈에는 스레드를 생성할 수 없다")
        void createThread_Expired_Rejected() {
            // given
            Buzz expired = withId(Buzz.create(creatorId, "본문", List.of(), Set.of(starterId), LocalDateTime.now().minusHours(7)), 2L);
            given(buzzRepository.findByIdForShare(expired.getId())).willReturn(Optional.of(expired));
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(expired.getId(), starterId))
                    .willReturn(Optional.of(BuzzAudienceMember.create(expired.getId(), starterId)));

            // when & then
            assertThatThrownBy(() -> buzzThreadService.createThread(new CreateBuzzThreadCommand(
                    starterId, expired.getId(), ThreadVisibility.PUBLIC, "첫 글", List.of())))
                    .isInstanceOf(BuzzInvalidStateException.class);
        }

        private CreateBuzzThreadCommand command(Long userId, ThreadVisibility visibility) {
            return new CreateBuzzThreadCommand(userId, buzz.getId(), visibility, "첫 글", List.of("buzz/a"));
        }
    }

    @Nested
    @DisplayName("첫 글 수정과 스레드 삭제")
    class Modification {

        @Test
        @DisplayName("시작자가 첫 글을 수정하면 스레드의 본문과 이미지 키가 바뀐다")
        void updateThread_ByStarter() {
            // given
            BuzzThread thread = thread(ThreadVisibility.PUBLIC);
            given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread));

            // when
            buzzThreadService.updateThread(new UpdateBuzzThreadCommand(
                    starterId, buzz.getId(), 10L, "수정한 글", List.of("buzz/b")));

            // then
            assertThat(thread.getText()).isEqualTo("수정한 글");
            assertThat(thread.getImageKeys()).containsExactly("buzz/b");
            verify(buzzThreadRepository).save(thread);
        }

        @Test
        @DisplayName("게시자가 스레드를 삭제하면 스레드를 먼저 지운 뒤 답글을 지운다")
        void deleteThread_ByCreator_DeletesThreadThenComments() {
            // given
            given(buzzRepository.findByIdForShare(buzz.getId())).willReturn(Optional.of(buzz));
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread(ThreadVisibility.PRIVATE)));

            // when
            buzzThreadService.deleteThread(creatorId, buzz.getId(), 10L);

            // then
            InOrder order = inOrder(buzzThreadRepository, buzzCommentRepository);
            order.verify(buzzThreadRepository).deleteById(10L);
            order.verify(buzzCommentRepository).deleteAllByThreadId(10L);
        }

        @Test
        @DisplayName("시작자도 게시자도 아닌 멤버는 스레드를 삭제할 수 없다")
        void deleteThread_ByOtherMember_Denied() {
            // given
            given(buzzRepository.findByIdForShare(buzz.getId())).willReturn(Optional.of(buzz));
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread(ThreadVisibility.PUBLIC)));

            // when & then
            assertThatThrownBy(() -> buzzThreadService.deleteThread(friendMemberId, buzz.getId(), 10L))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzThreadRepository, never()).deleteById(any());
        }

    }

    private BuzzThread thread(ThreadVisibility visibility) {
        BuzzThread thread = BuzzThread.create(buzz, starterId, true, visibility, "첫 글", List.of(), LocalDateTime.now());
        ReflectionTestUtils.setField(thread, "id", 10L);
        return thread;
    }

    private static Buzz withId(Buzz buzz, long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(buzz, "id", id);
        return buzz;
    }
}
