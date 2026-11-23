package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.view.BuzzAudienceView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzDetailView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzSummaryView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzThreadView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzUserProfileView;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

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
class BuzzQueryServiceTest {

    @Mock private BuzzQueryRepository buzzQueryRepository;
    @Mock private BuzzSocialPort buzzSocialPort;
    @Mock private ImageStoragePort imageStoragePort;

    private BuzzQueryService buzzQueryService;

    private final Long creatorId = 1L;
    private final Long recipientId = 2L;
    private final Long strangerId = 99L;
    private final Long buzzId = 1L;
    private final BuzzUserProfileView creatorProfile = new BuzzUserProfileView("작성자", "profiles/creator");

    @BeforeEach
    void setUp() {
        buzzQueryService = new BuzzQueryService(
                buzzQueryRepository, buzzSocialPort, new BuzzProfileAssembler(imageStoragePort), imageStoragePort);
    }

    @Nested
    @DisplayName("버즈 상세 조회")
    class GetBuzzDetail {

        private void givenDetail(BuzzThreadView... threads) {
            given(buzzQueryRepository.findDetail(buzzId)).willReturn(Optional.of(new BuzzDetailView(
                    buzzId, creatorId, creatorProfile, "본문", List.of("buzz/photo"), null,
                    List.of(threads))));
        }

        private void givenAudience(BuzzAudienceView... audience) {
            given(buzzQueryRepository.findAudience(buzzId)).willReturn(List.of(audience));
        }

        private BuzzAudienceView member(Long userId, boolean isRead) {
            return new BuzzAudienceView(userId, new BuzzUserProfileView("멤버" + userId, null), isRead);
        }

        @Test
        @DisplayName("미읽음 멤버가 조회하면 미읽음으로 응답하고 작성자 프로필 이미지를 서명한다")
        void getBuzzDetail_UnreadMember_RespondsUnread() {
            // given
            givenDetail();
            givenAudience(member(recipientId, false));
            given(imageStoragePort.resolveUrl("profiles/creator")).willReturn("signed-creator");

            // when
            BuzzDetailResult result = buzzQueryService.getBuzzDetail(recipientId, buzzId);

            // then
            assertThat(result.isUnread()).isTrue();
            assertThat(result.isCreator()).isFalse();
            assertThat(result.author().nickname()).isEqualTo("작성자");
            assertThat(result.author().profileImageUrl()).isEqualTo("signed-creator");
            assertThat(result.recipients()).extracting("userId").containsExactly(recipientId);
        }

        @Test
        @DisplayName("게시자가 조회하면 응답을 만들 때 저장된 이미지 키를 서명하고 미읽음이 아니다")
        void getBuzzDetail_Creator_ResolvesImageKeys() {
            // given
            givenDetail();
            givenAudience(member(recipientId, false));
            given(imageStoragePort.resolveUrls(List.of("buzz/photo"))).willReturn(List.of("signed-photo"));

            // when
            BuzzDetailResult result = buzzQueryService.getBuzzDetail(creatorId, buzzId);

            // then
            assertThat(result.imageUrls()).containsExactly("signed-photo");
            assertThat(result.isCreator()).isTrue();
            assertThat(result.isUnread()).isFalse();
        }

        @Test
        @DisplayName("프로필이 없는 공개 대상은 수신자 목록에서 빠지지만 접근 확인에는 쓰인다")
        void getBuzzDetail_MemberWithoutProfile_ExcludedFromRecipients() {
            // given
            givenDetail();
            givenAudience(member(recipientId, true), new BuzzAudienceView(3L, null, false));

            // when
            BuzzDetailResult result = buzzQueryService.getBuzzDetail(3L, buzzId);

            // then
            assertThat(result.isUnread()).isTrue();
            assertThat(result.recipients()).extracting("userId").containsExactly(recipientId);
        }

        @Test
        @DisplayName("게시자도 공개 대상 멤버도 아니면 접근이 거절된다")
        void getBuzzDetail_Stranger_Denied() {
            givenDetail();
            givenAudience(member(recipientId, false));

            assertThatThrownBy(() -> buzzQueryService.getBuzzDetail(strangerId, buzzId))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("상세에는 열람자가 접근할 수 있는 스레드만 시작자와 함께 담긴다")
        void getBuzzDetail_ListsOnlyAccessibleThreads() {
            // given
            Long friendStarterId = 3L;
            Long strangerStarterId = 4L;
            givenDetail(
                    thread(1L, recipientId, ThreadVisibility.PRIVATE),
                    thread(2L, friendStarterId, ThreadVisibility.PUBLIC),
                    thread(3L, friendStarterId, ThreadVisibility.PRIVATE),
                    thread(4L, strangerStarterId, ThreadVisibility.PUBLIC));
            givenAudience(member(recipientId, true));
            given(buzzSocialPort.getFriendIdsIn(recipientId, Set.of(friendStarterId, strangerStarterId)))
                    .willReturn(Set.of(friendStarterId));

            // when
            BuzzDetailResult result = buzzQueryService.getBuzzDetail(recipientId, buzzId);

            // then
            assertThat(result.threads()).extracting("threadId").containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("게시자는 친구 관계 확인 없이 모든 스레드를 본다")
        void getBuzzDetail_CreatorSeesAllThreads() {
            // given
            givenDetail(
                    thread(1L, recipientId, ThreadVisibility.PUBLIC),
                    thread(2L, recipientId, ThreadVisibility.PRIVATE));
            givenAudience(member(recipientId, true));

            // when
            BuzzDetailResult result = buzzQueryService.getBuzzDetail(creatorId, buzzId);

            // then
            assertThat(result.threads()).extracting("threadId").containsExactly(1L, 2L);
            verify(buzzSocialPort, never()).getFriendIdsIn(any(), any());
        }

        @Test
        @DisplayName("조회 가능한 버즈가 없으면 찾을 수 없다")
        void getBuzzDetail_Missing_NotFound() {
            given(buzzQueryRepository.findDetail(buzzId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> buzzQueryService.getBuzzDetail(creatorId, buzzId))
                    .isInstanceOf(BuzzNotFoundException.class);
            verify(buzzQueryRepository, never()).findAudience(any());
        }

        private BuzzThreadView thread(Long threadId, Long starterId, ThreadVisibility visibility) {
            return new BuzzThreadView(threadId, starterId, null, visibility, LocalDateTime.now());
        }
    }

    @Test
    @DisplayName("받은 목록은 멤버의 읽음 상태와 조회한 작성자 프로필로 만든다")
    void getReceivedBuzzes_MapsReadStateAndProfile() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        given(buzzSocialPort.getNonReceivableFriendIds(recipientId)).willReturn(Set.of(7L));
        given(buzzQueryRepository.findReceived(recipientId, Set.of(7L), pageable))
                .willReturn(new SliceImpl<>(List.of(summary(creatorProfile, false)), pageable, false));
        given(imageStoragePort.resolveUrl("profiles/creator")).willReturn("signed-creator");

        // when
        Slice<BuzzSummaryResult> result = buzzQueryService.getReceivedBuzzes(recipientId, pageable);

        // then
        BuzzSummaryResult received = result.getContent().get(0);
        assertThat(received.isUnread()).isTrue();
        assertThat(received.isCreator()).isFalse();
        assertThat(received.author().nickname()).isEqualTo("작성자");
        assertThat(received.author().profileImageUrl()).isEqualTo("signed-creator");
    }

    @Test
    @DisplayName("작성자 프로필이 없으면 받은 목록에 사용자 ID만 담는다")
    void getReceivedBuzzes_WithoutProfile_KeepsUserIdOnly() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        given(buzzSocialPort.getNonReceivableFriendIds(recipientId)).willReturn(Set.of());
        given(buzzQueryRepository.findReceived(recipientId, Set.of(), pageable))
                .willReturn(new SliceImpl<>(List.of(summary(null, true)), pageable, false));

        // when
        Slice<BuzzSummaryResult> result = buzzQueryService.getReceivedBuzzes(recipientId, pageable);

        // then
        assertThat(result.getContent().get(0).author().userId()).isEqualTo(creatorId);
        assertThat(result.getContent().get(0).author().nickname()).isNull();
    }

    @Test
    @DisplayName("보낸 목록은 자기 버즈를 게시자 표시와 함께 준다")
    void getSentBuzzes_MarksCreator() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        given(buzzQueryRepository.findSent(creatorId, pageable))
                .willReturn(new SliceImpl<>(List.of(summary(creatorProfile, false)), pageable, false));

        // when
        Slice<BuzzSummaryResult> result = buzzQueryService.getSentBuzzes(creatorId, pageable);

        // then
        assertThat(result.getContent().get(0).isCreator()).isTrue();
        assertThat(result.getContent().get(0).isUnread()).isFalse();
    }

    private BuzzSummaryView summary(BuzzUserProfileView profile, boolean isRead) {
        return new BuzzSummaryView(buzzId, creatorId, profile, "본문", List.of(), null, isRead);
    }
}
