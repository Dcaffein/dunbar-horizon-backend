package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.view.BuzzAudienceView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzCommentView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzThreadDetailView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzThreadDetailView.ThreadContent;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzUserProfileView;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadDetailResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class BuzzThreadQueryServiceTest {

    @Mock private BuzzQueryRepository buzzQueryRepository;
    @Mock private BuzzSocialPort buzzSocialPort;
    @Mock private ImageStoragePort imageStoragePort;

    private BuzzThreadQueryService buzzThreadQueryService;

    private final Long buzzId = 1L;
    private final Long threadId = 10L;
    private final Long creatorId = 1L;
    private final Long starterId = 2L;
    private final Long friendMemberId = 3L;
    private final Long otherMemberId = 4L;

    @BeforeEach
    void setUp() {
        buzzThreadQueryService = new BuzzThreadQueryService(buzzQueryRepository, buzzSocialPort,
                new BuzzProfileAssembler(imageStoragePort), imageStoragePort);
    }

    private void givenThread(ThreadVisibility visibility) {
        givenDetail(new ThreadContent(threadId, starterId, profile("시작자"), visibility, "첫 글", List.of(),
                LocalDateTime.now()));
    }

    private void givenDetail(ThreadContent thread) {
        given(buzzQueryRepository.findThreadDetail(buzzId, threadId))
                .willReturn(Optional.of(new BuzzThreadDetailView(buzzId, creatorId, profile("게시자"), thread)));
    }

    private void givenAudience() {
        given(buzzQueryRepository.findAudience(buzzId)).willReturn(List.of(
                new BuzzAudienceView(starterId, profile("시작자"), true),
                new BuzzAudienceView(friendMemberId, profile("친구"), true),
                new BuzzAudienceView(otherMemberId, profile("다른 멤버"), true)));
    }

    private static BuzzUserProfileView profile(String nickname) {
        return new BuzzUserProfileView(nickname, null);
    }

    @Nested
    @DisplayName("스레드 조회")
    class GetThread {

        @Test
        @DisplayName("시작자의 친구인 멤버는 공개 스레드를 보고, 볼 수 있는 사람 목록에는 게시자·시작자·시작자의 친구 멤버가 담긴다")
        void getThread_PublicByFriend_ListsViewers() {
            // given
            givenThread(ThreadVisibility.PUBLIC);
            givenAudience();
            given(buzzSocialPort.getFriendIdsIn(starterId, Set.of(friendMemberId, otherMemberId)))
                    .willReturn(Set.of(friendMemberId));

            // when
            BuzzThreadDetailResult result = buzzThreadQueryService.getThread(friendMemberId, buzzId, threadId);

            // then
            assertThat(result.viewers()).extracting("userId").containsExactly(creatorId, starterId, friendMemberId);
            assertThat(result.isMine()).isFalse();
        }

        @Test
        @DisplayName("시작자의 친구가 아닌 멤버는 공개 스레드를 볼 수 없고 답글도 읽지 않는다")
        void getThread_PublicByNonFriend_Denied() {
            // given
            givenThread(ThreadVisibility.PUBLIC);
            givenAudience();
            given(buzzSocialPort.getFriendIdsIn(starterId, Set.of(friendMemberId, otherMemberId)))
                    .willReturn(Set.of(friendMemberId));

            // when & then
            assertThatThrownBy(() -> buzzThreadQueryService.getThread(otherMemberId, buzzId, threadId))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzQueryRepository, never()).findComments(any());
        }

        @Test
        @DisplayName("시작자의 친구인 멤버도 비공개 스레드는 볼 수 없다")
        void getThread_PrivateByFriend_Denied() {
            // given
            givenThread(ThreadVisibility.PRIVATE);
            givenAudience();

            // when & then
            assertThatThrownBy(() -> buzzThreadQueryService.getThread(friendMemberId, buzzId, threadId))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzSocialPort, never()).getFriendIdsIn(any(), any());
        }

        @Test
        @DisplayName("비공개 스레드를 볼 수 있는 사람은 게시자와 시작자뿐이다")
        void getThread_PrivateByCreator_ViewersAreCreatorAndStarter() {
            // given
            givenThread(ThreadVisibility.PRIVATE);
            givenAudience();

            // when
            BuzzThreadDetailResult result = buzzThreadQueryService.getThread(creatorId, buzzId, threadId);

            // then
            assertThat(result.viewers()).extracting("userId").containsExactly(creatorId, starterId);
        }

        @Test
        @DisplayName("프로필이 없는 사용자는 볼 수 있는 사람 목록에서 빠진다")
        void getThread_ViewerWithoutProfile_Excluded() {
            // given
            givenDetail(new ThreadContent(threadId, starterId, null, ThreadVisibility.PRIVATE, "첫 글", List.of(),
                    LocalDateTime.now()));
            given(buzzQueryRepository.findAudience(buzzId))
                    .willReturn(List.of(new BuzzAudienceView(starterId, null, true)));

            // when
            BuzzThreadDetailResult result = buzzThreadQueryService.getThread(creatorId, buzzId, threadId);

            // then
            assertThat(result.viewers()).extracting("userId").containsExactly(creatorId);
            assertThat(result.starter().userId()).isEqualTo(starterId);
            assertThat(result.starter().nickname()).isNull();
        }

        @Test
        @DisplayName("시작자의 친구라도 게시자나 공개 대상 멤버가 아니면 접근할 수 없다")
        void getThread_FriendOutsideAudience_Denied() {
            // given
            givenThread(ThreadVisibility.PUBLIC);
            givenAudience();

            // when & then
            assertThatThrownBy(() -> buzzThreadQueryService.getThread(50L, buzzId, threadId))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzSocialPort, never()).getFriendIdsIn(any(), any());
        }

        @Test
        @DisplayName("버즈는 있지만 그 버즈의 스레드가 없으면 스레드를 찾을 수 없다")
        void getThread_ThreadMissing_NotFound() {
            // given
            givenDetail(null);
            givenAudience();

            // when & then
            assertThatThrownBy(() -> buzzThreadQueryService.getThread(creatorId, buzzId, threadId))
                    .isInstanceOf(BuzzThreadNotFoundException.class);
        }

        @Test
        @DisplayName("조회 가능한 버즈가 없으면 버즈를 찾을 수 없다")
        void getThread_BuzzMissing_NotFound() {
            // given
            given(buzzQueryRepository.findThreadDetail(buzzId, threadId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buzzThreadQueryService.getThread(creatorId, buzzId, threadId))
                    .isInstanceOf(BuzzNotFoundException.class);
        }
    }

    @Test
    @DisplayName("스레드 조회는 답글을 작성자 프로필과 본인 여부와 함께 돌려준다")
    void getThread_IncludesComments() {
        // given
        givenThread(ThreadVisibility.PRIVATE);
        givenAudience();
        given(buzzQueryRepository.findComments(threadId)).willReturn(List.of(
                new BuzzCommentView(100L, creatorId, profile("게시자"), "게시자 답글", List.of(), LocalDateTime.now()),
                new BuzzCommentView(101L, starterId, profile("시작자"), "시작자 답글", List.of(), LocalDateTime.now())));

        // when
        BuzzThreadDetailResult result = buzzThreadQueryService.getThread(creatorId, buzzId, threadId);

        // then
        assertThat(result.comments()).extracting("text").containsExactly("게시자 답글", "시작자 답글");
        assertThat(result.comments()).extracting("isMine").containsExactly(true, false);
        assertThat(result.comments().get(1).author().nickname()).isEqualTo("시작자");
    }
}
