package com.example.DunbarHorizon.buzz.application.service;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuzzThreadQueryServiceTest {

    @Mock private BuzzRepository buzzRepository;
    @Mock private BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    @Mock private BuzzThreadRepository buzzThreadRepository;
    @Mock private BuzzCommentRepository buzzCommentRepository;
    @Mock private BuzzSocialPort buzzSocialPort;
    @Mock private ImageStoragePort imageStoragePort;

    private BuzzThreadQueryService buzzThreadQueryService;

    private final Long creatorId = 1L;
    private final Long starterId = 2L;
    private final Long friendMemberId = 3L;
    private final Long otherMemberId = 4L;
    private Buzz buzz;

    @BeforeEach
    void setUp() {
        buzzThreadQueryService = new BuzzThreadQueryService(buzzAudienceMemberRepository, buzzCommentRepository,
                new BuzzThreadAccess(buzzRepository, buzzAudienceMemberRepository, buzzThreadRepository, buzzSocialPort),
                buzzSocialPort, new BuzzProfileAssembler(buzzSocialPort), imageStoragePort);
        buzz = withId(Buzz.create(creatorId, "본문", List.of(),
                Set.of(starterId, friendMemberId, otherMemberId), LocalDateTime.now()), 1L);
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
            BuzzThreadDetailResult result = buzzThreadQueryService.getThread(friendMemberId, buzz.getId(), 10L);

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
            assertThatThrownBy(() -> buzzThreadQueryService.getThread(otherMemberId, buzz.getId(), 10L))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("시작자의 친구인 멤버도 비공개 스레드는 볼 수 없다")
        void getThread_PrivateByFriend_Denied() {
            // given
            givenMember(friendMemberId);
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(thread(ThreadVisibility.PRIVATE)));

            // when & then
            assertThatThrownBy(() -> buzzThreadQueryService.getThread(friendMemberId, buzz.getId(), 10L))
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
            BuzzThreadDetailResult result = buzzThreadQueryService.getThread(creatorId, buzz.getId(), 10L);

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
            assertThatThrownBy(() -> buzzThreadQueryService.getThread(outsiderId, buzz.getId(), 10L))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzThreadRepository, never()).findById(any());
        }

        @Test
        @DisplayName("다른 버즈의 스레드 ID로 조회하면 찾을 수 없다")
        void getThread_OtherBuzzThread_NotFound() {
            // given
            Buzz otherBuzz = withId(Buzz.create(creatorId, "다른 본문", List.of(), Set.of(starterId), LocalDateTime.now()), 3L);
            BuzzThread otherBuzzThread = BuzzThread.create(otherBuzz, starterId, true, ThreadVisibility.PUBLIC,
                    "첫 글", List.of(), LocalDateTime.now());
            given(buzzThreadRepository.findById(10L)).willReturn(Optional.of(otherBuzzThread));

            // when & then
            assertThatThrownBy(() -> buzzThreadQueryService.getThread(creatorId, buzz.getId(), 10L))
                    .isInstanceOf(BuzzThreadNotFoundException.class);
        }

        private void givenMember(Long userId) {
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), userId))
                    .willReturn(Optional.of(BuzzAudienceMember.create(buzz.getId(), userId)));
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
        BuzzThreadDetailResult result = buzzThreadQueryService.getThread(creatorId, buzz.getId(), 10L);

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

    private static Buzz withId(Buzz buzz, long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(buzz, "id", id);
        return buzz;
    }
}
