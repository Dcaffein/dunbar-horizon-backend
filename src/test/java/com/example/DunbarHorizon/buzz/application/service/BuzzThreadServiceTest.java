package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadDetailResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import com.example.DunbarHorizon.buzz.domain.event.BuzzThreadCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadAlreadyExistsException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyList;
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
    @Mock private BuzzSocialPort buzzSocialPort;
    @Mock private ImageStoragePort imageStoragePort;
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
                buzzCommentRepository,
                new BuzzThreadAccess(buzzRepository, buzzAudienceMemberRepository, buzzThreadRepository, buzzSocialPort),
                buzzSocialPort, new BuzzProfileAssembler(buzzSocialPort), imageStoragePort, applicationEventPublisher);
        buzz = Buzz.create(creatorId, "본문", List.of(),
                Set.of(starterId, friendMemberId, otherMemberId), LocalDateTime.now());
    }

    @Nested
    @DisplayName("스레드 생성")
    class CreateThread {

        @Test
        @DisplayName("공개 대상 멤버가 스레드를 생성하면 저장하고 그 멤버를 읽음으로 바꾼다")
        void createThread_ByMember_SavesAndMarksRead() {
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
            assertThat(member.isUnread()).isFalse();
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
            Buzz expired = Buzz.create(creatorId, "본문", List.of(), Set.of(starterId), LocalDateTime.now().minusHours(7));
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
    @DisplayName("스레드 조회")
    class GetThread {

        @BeforeEach
        void setUp() {
            given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));
        }

        @Test
        @DisplayName("시작자의 친구인 멤버는 공개 스레드를 보고, 볼 수 있는 사람 목록에는 게시자·시작자·시작자의 친구 멤버가 담긴다")
        void getThread_PublicByFriend_ListsViewers() {
            // given
            BuzzThread thread = thread(ThreadVisibility.PUBLIC);
            givenMember(friendMemberId);
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread));
            given(buzzSocialPort.getFriendIdsIn(friendMemberId, List.of(starterId))).willReturn(Set.of(starterId));
            given(buzzAudienceMemberRepository.findUserIdsByBuzzId(buzz.getId()))
                    .willReturn(List.of(starterId, friendMemberId, otherMemberId));
            given(buzzSocialPort.getFriendIdsIn(starterId, List.of(starterId, friendMemberId, otherMemberId)))
                    .willReturn(Set.of(friendMemberId));
            given(buzzSocialPort.getCreatorProfiles(anyList())).willReturn(List.of(
                    new BuzzCreatorInfo(creatorId, "게시자", null),
                    new BuzzCreatorInfo(starterId, "시작자", null),
                    new BuzzCreatorInfo(friendMemberId, "친구", null)));

            // when
            BuzzThreadDetailResult result = buzzThreadService.getThread(friendMemberId, buzz.getId(), 10L);

            // then
            assertThat(result.viewers()).extracting("userId").containsExactly(creatorId, starterId, friendMemberId);
            assertThat(result.isMine()).isFalse();
        }

        @Test
        @DisplayName("시작자의 친구가 아닌 멤버는 공개 스레드를 볼 수 없다")
        void getThread_PublicByNonFriend_Denied() {
            // given
            givenMember(otherMemberId);
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread(ThreadVisibility.PUBLIC)));
            given(buzzSocialPort.getFriendIdsIn(otherMemberId, List.of(starterId))).willReturn(Set.of());

            // when & then
            assertThatThrownBy(() -> buzzThreadService.getThread(otherMemberId, buzz.getId(), 10L))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("시작자의 친구인 멤버도 비공개 스레드는 볼 수 없다")
        void getThread_PrivateByFriend_Denied() {
            // given
            givenMember(friendMemberId);
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread(ThreadVisibility.PRIVATE)));

            // when & then
            assertThatThrownBy(() -> buzzThreadService.getThread(friendMemberId, buzz.getId(), 10L))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzSocialPort, never()).getFriendIdsIn(any(), any());
        }

        @Test
        @DisplayName("비공개 스레드를 볼 수 있는 사람은 게시자와 시작자뿐이다")
        void getThread_PrivateByCreator_ViewersAreCreatorAndStarter() {
            // given
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread(ThreadVisibility.PRIVATE)));
            given(buzzSocialPort.getCreatorProfiles(anyList())).willReturn(List.of(
                    new BuzzCreatorInfo(creatorId, "게시자", null),
                    new BuzzCreatorInfo(starterId, "시작자", null)));

            // when
            BuzzThreadDetailResult result = buzzThreadService.getThread(creatorId, buzz.getId(), 10L);

            // then
            assertThat(result.viewers()).extracting("userId").containsExactly(creatorId, starterId);
        }

        @Test
        @DisplayName("시작자의 친구라도 게시자나 공개 대상 멤버가 아니면 접근할 수 없다")
        void getThread_FriendOutsideAudience_Denied() {
            // given
            Long outsiderId = 50L;
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), outsiderId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buzzThreadService.getThread(outsiderId, buzz.getId(), 10L))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzThreadRepository, never()).findById(any());
        }

        @Test
        @DisplayName("다른 버즈의 스레드 ID로 조회하면 찾을 수 없다")
        void getThread_OtherBuzzThread_NotFound() {
            // given
            Buzz otherBuzz = Buzz.create(creatorId, "다른 본문", List.of(), Set.of(starterId), LocalDateTime.now());
            BuzzThread otherBuzzThread = BuzzThread.create(otherBuzz, starterId, true, ThreadVisibility.PUBLIC,
                    "첫 글", List.of(), LocalDateTime.now());
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(otherBuzzThread));

            // when & then
            assertThatThrownBy(() -> buzzThreadService.getThread(creatorId, buzz.getId(), 10L))
                    .isInstanceOf(BuzzThreadNotFoundException.class);
        }

        private void givenMember(Long userId) {
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), userId))
                    .willReturn(Optional.of(BuzzAudienceMember.create(buzz.getId(), userId)));
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

    @Test
    @DisplayName("스레드 조회는 답글을 작성자 프로필과 본인 여부와 함께 돌려준다")
    void getThread_IncludesComments() {
        // given
        given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));
        BuzzThread thread = thread(ThreadVisibility.PRIVATE);
        given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread));
        BuzzComment mine = BuzzComment.write(buzz, thread, creatorId, "게시자 답글", List.of(), LocalDateTime.now());
        BuzzComment starterReply = BuzzComment.write(buzz, thread, starterId, "시작자 답글", List.of(), LocalDateTime.now());
        given(buzzCommentRepository.findAllByThreadId(10L)).willReturn(List.of(mine, starterReply));
        given(buzzSocialPort.getCreatorProfiles(anyList())).willReturn(List.of(
                new BuzzCreatorInfo(creatorId, "게시자", null),
                new BuzzCreatorInfo(starterId, "시작자", null)));

        // when
        BuzzThreadDetailResult result = buzzThreadService.getThread(creatorId, buzz.getId(), 10L);

        // then
        assertThat(result.comments()).extracting("text").containsExactly("게시자 답글", "시작자 답글");
        assertThat(result.comments()).extracting("isMine").containsExactly(true, false);
        assertThat(result.comments().get(1).author().nickname()).isEqualTo("시작자");
    }

    private BuzzThread thread(ThreadVisibility visibility) {
        BuzzThread thread = BuzzThread.create(buzz, starterId, true, visibility, "첫 글", List.of(), LocalDateTime.now());
        ReflectionTestUtils.setField(thread, "id", 10L);
        return thread;
    }
}
