package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.ManualRecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuzzServiceTest {

    @Mock private RecipientStrategyProvider recipientStrategyProvider;
    @Mock private BuzzRepository buzzRepository;
    @Mock private BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    @Mock private BuzzThreadRepository buzzThreadRepository;
    @Mock private BuzzQueryRepository buzzQueryRepository;
    @Mock private BuzzSocialPort buzzSocialPort;
    @Mock private ImageStoragePort imageStoragePort;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private RecipientStrategyPort recipientStrategyPort;

    private BuzzService buzzService;

    private final Long creatorId = 1L;
    private final Long recipientId = 2L;
    private final Long strangerId = 99L;

    @BeforeEach
    void setUp() {
        buzzService = new BuzzService(recipientStrategyProvider, buzzRepository, buzzAudienceMemberRepository,
                buzzThreadRepository, buzzQueryRepository, buzzSocialPort, new BuzzProfileAssembler(buzzSocialPort),
                imageStoragePort, applicationEventPublisher);
    }

    private Buzz buzz(List<String> imageKeys) {
        return Buzz.create(creatorId, "본문", imageKeys, Set.of(recipientId), LocalDateTime.now());
    }

    @Nested
    @DisplayName("버즈 생성")
    class CreateBuzz {

        @BeforeEach
        void setUp() {
            given(recipientStrategyProvider.getStrategy(RecipientType.MANUAL)).willReturn(recipientStrategyPort);
            given(recipientStrategyPort.fetchRecipientIds(eq(creatorId), any())).willReturn(Set.of(recipientId, 3L));
        }

        @Test
        @DisplayName("이미지 키를 서명하지 않고 그대로 저장한다")
        void createBuzz_SavesImageKeys() {
            // when
            buzzService.createBuzz(command(), List.of("buzz/photo"));

            // then
            ArgumentCaptor<Buzz> buzzCaptor = ArgumentCaptor.forClass(Buzz.class);
            verify(buzzRepository).save(buzzCaptor.capture());
            assertThat(buzzCaptor.getValue().getImageKeys()).containsExactly("buzz/photo");
            verify(imageStoragePort, never()).resolveUrls(anyList());
        }

        @Test
        @DisplayName("지정 결과의 사용자마다 미읽음 공개 대상 멤버를 저장하고 생성 이벤트를 발행한다")
        @SuppressWarnings("unchecked")
        void createBuzz_SavesAudienceMembersAndPublishesEvent() {
            // when
            buzzService.createBuzz(command(), List.of());

            // then
            ArgumentCaptor<Buzz> buzzCaptor = ArgumentCaptor.forClass(Buzz.class);
            verify(buzzRepository).save(buzzCaptor.capture());
            String buzzId = buzzCaptor.getValue().getId();

            ArgumentCaptor<List<BuzzAudienceMember>> memberCaptor = ArgumentCaptor.forClass(List.class);
            verify(buzzAudienceMemberRepository).saveAll(memberCaptor.capture());
            assertThat(memberCaptor.getValue())
                    .extracting(BuzzAudienceMember::getBuzzId, BuzzAudienceMember::getUserId, BuzzAudienceMember::isUnread)
                    .containsExactlyInAnyOrder(tuple(buzzId, recipientId, true), tuple(buzzId, 3L, true));
            verify(applicationEventPublisher)
                    .publishEvent(new BuzzCreatedEvent(buzzId, creatorId, Set.of(recipientId, 3L)));
        }

        private CreateBuzzCommand command() {
            return new CreateBuzzCommand(creatorId, "텍스트", new ManualRecipientSpec(List.of(recipientId, 3L)));
        }
    }

    @Nested
    @DisplayName("버즈 상세 조회")
    class GetBuzzDetail {

        private Buzz buzz;

        @BeforeEach
        void setUp() {
            buzz = buzz(List.of("buzz/photo"));
            given(buzzRepository.findByIdForShare(buzz.getId())).willReturn(Optional.of(buzz));
        }

        @Test
        @DisplayName("미읽음 멤버가 열람하면 미읽음으로 응답하고 그 멤버를 읽음으로 바꾼다")
        void getBuzzDetail_UnreadMember_MarksAsRead() {
            // given
            BuzzAudienceMember member = BuzzAudienceMember.create(buzz.getId(), recipientId);
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), recipientId))
                    .willReturn(Optional.of(member));
            given(buzzAudienceMemberRepository.findUserIdsByBuzzId(buzz.getId())).willReturn(List.of(recipientId));
            given(buzzSocialPort.getCreatorProfiles(anyList())).willReturn(List.of(
                    new BuzzCreatorInfo(creatorId, "작성자", "signed-creator"),
                    new BuzzCreatorInfo(recipientId, "수신자", "signed-recipient")));

            // when
            BuzzDetailResult result = buzzService.getBuzzDetail(recipientId, buzz.getId());

            // then
            assertThat(result.isUnread()).isTrue();
            assertThat(result.isCreator()).isFalse();
            assertThat(member.isUnread()).isFalse();
            assertThat(result.author().profileImageUrl()).isEqualTo("signed-creator");
            assertThat(result.recipients()).extracting("userId").containsExactly(recipientId);
        }

        @Test
        @DisplayName("응답을 만들 때 저장된 이미지 키를 서명한다")
        void getBuzzDetail_ResolvesImageKeysAtResponse() {
            // given
            given(buzzAudienceMemberRepository.findUserIdsByBuzzId(buzz.getId())).willReturn(List.of(recipientId));
            given(imageStoragePort.resolveUrls(List.of("buzz/photo"))).willReturn(List.of("signed-photo"));

            // when
            BuzzDetailResult result = buzzService.getBuzzDetail(creatorId, buzz.getId());

            // then
            assertThat(result.imageUrls()).containsExactly("signed-photo");
            assertThat(result.isCreator()).isTrue();
            assertThat(result.isUnread()).isFalse();
            verify(buzzAudienceMemberRepository, never()).findByBuzzIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("게시자도 공개 대상 멤버도 아니면 접근이 거절된다")
        void getBuzzDetail_Stranger_Denied() {
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), strangerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> buzzService.getBuzzDetail(strangerId, buzz.getId()))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }

        @Test
        @DisplayName("상세에는 열람자가 접근할 수 있는 스레드만 시작자와 함께 담긴다")
        void getBuzzDetail_ListsOnlyAccessibleThreads() {
            // given
            Long friendStarterId = 3L;
            Long strangerStarterId = 4L;
            BuzzThread own = thread(recipientId, ThreadVisibility.PRIVATE, 1L);
            BuzzThread friendPublic = thread(friendStarterId, ThreadVisibility.PUBLIC, 2L);
            BuzzThread friendPrivate = thread(friendStarterId, ThreadVisibility.PRIVATE, 3L);
            BuzzThread strangerPublic = thread(strangerStarterId, ThreadVisibility.PUBLIC, 4L);
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), recipientId))
                    .willReturn(Optional.of(BuzzAudienceMember.create(buzz.getId(), recipientId)));
            given(buzzThreadRepository.findAllByBuzzId(buzz.getId()))
                    .willReturn(List.of(own, friendPublic, friendPrivate, strangerPublic));
            given(buzzSocialPort.getFriendIdsIn(recipientId, Set.of(friendStarterId, strangerStarterId)))
                    .willReturn(Set.of(friendStarterId));

            // when
            BuzzDetailResult result = buzzService.getBuzzDetail(recipientId, buzz.getId());

            // then
            assertThat(result.threads()).extracting("threadId").containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("게시자는 친구 관계 확인 없이 모든 스레드를 본다")
        void getBuzzDetail_CreatorSeesAllThreads() {
            // given
            given(buzzThreadRepository.findAllByBuzzId(buzz.getId())).willReturn(List.of(
                    thread(recipientId, ThreadVisibility.PUBLIC, 1L),
                    thread(recipientId, ThreadVisibility.PRIVATE, 2L)));

            // when
            BuzzDetailResult result = buzzService.getBuzzDetail(creatorId, buzz.getId());

            // then
            assertThat(result.threads()).extracting("threadId").containsExactly(1L, 2L);
            verify(buzzSocialPort, never()).getFriendIdsIn(any(), any());
        }

        private BuzzThread thread(Long starterId, ThreadVisibility visibility, Long id) {
            BuzzThread thread = BuzzThread.open(buzz.getId(), starterId, visibility, "첫 글", List.of(), LocalDateTime.now());
            org.springframework.test.util.ReflectionTestUtils.setField(thread, "id", id);
            return thread;
        }
    }

    @Test
    @DisplayName("만료된 버즈는 상세 조회에서 찾을 수 없다")
    void getBuzzDetail_Expired_NotFound() {
        // given
        Buzz expired = Buzz.create(creatorId, "본문", List.of(), Set.of(recipientId), LocalDateTime.now().minusHours(7));
        given(buzzRepository.findByIdForShare(expired.getId())).willReturn(Optional.of(expired));

        // when & then
        assertThatThrownBy(() -> buzzService.getBuzzDetail(creatorId, expired.getId()))
                .isInstanceOf(BuzzNotFoundException.class);
    }

    @Test
    @DisplayName("받은 목록은 멤버의 읽음 상태와 응답 시점의 작성자 프로필로 만든다")
    void getReceivedBuzzes_MapsReadStateAndProfile() {
        // given
        Buzz buzz = buzz(List.of());
        PageRequest pageable = PageRequest.of(0, 20);
        given(buzzSocialPort.getNonReceivableFriendIds(recipientId)).willReturn(Set.of());
        given(buzzQueryRepository.findUnexpiredReceived(eq(recipientId), eq(Set.of()), any(), eq(pageable)))
                .willReturn(new SliceImpl<>(List.of(new ReceivedBuzz(buzz, false)), pageable, false));
        given(buzzSocialPort.getCreatorProfiles(List.of(creatorId)))
                .willReturn(List.of(new BuzzCreatorInfo(creatorId, "작성자", "signed-creator")));

        // when
        Slice<BuzzSummaryResult> result = buzzService.getReceivedBuzzes(recipientId, pageable);

        // then
        BuzzSummaryResult summary = result.getContent().get(0);
        assertThat(summary.isUnread()).isTrue();
        assertThat(summary.isCreator()).isFalse();
        assertThat(summary.author().nickname()).isEqualTo("작성자");
    }

    @Test
    @DisplayName("보낸 목록은 게시자 본인 기준으로 읽음 여부 없이 만든다")
    void getSentBuzzes_ReturnsCreatorView() {
        // given
        Buzz buzz = buzz(List.of());
        PageRequest pageable = PageRequest.of(0, 20);
        given(buzzQueryRepository.findUnexpiredSent(eq(creatorId), any(), eq(pageable)))
                .willReturn(new SliceImpl<>(List.of(buzz), pageable, false));
        given(buzzSocialPort.getCreatorProfiles(List.of(creatorId)))
                .willReturn(List.of(new BuzzCreatorInfo(creatorId, "작성자", null)));

        // when
        Slice<BuzzSummaryResult> result = buzzService.getSentBuzzes(creatorId, pageable);

        // then
        assertThat(result.getContent().get(0).isCreator()).isTrue();
        assertThat(result.getContent().get(0).isUnread()).isFalse();
    }

    @Nested
    @DisplayName("버즈 삭제")
    class DeleteBuzz {

        @Test
        @DisplayName("게시자가 삭제하면 버즈 행을 먼저 지운 뒤 공개 대상 멤버와 스레드를 지운다")
        void deleteBuzz_ByCreator_DeletesBuzzThenMembers() {
            // given
            Buzz buzz = buzz(List.of());
            given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));

            // when
            buzzService.deleteBuzz(creatorId, buzz.getId());

            // then
            InOrder order = inOrder(buzzRepository, buzzAudienceMemberRepository, buzzThreadRepository);
            order.verify(buzzRepository).deleteById(buzz.getId());
            order.verify(buzzAudienceMemberRepository).deleteAllByBuzzId(buzz.getId());
            order.verify(buzzThreadRepository).deleteAllByBuzzId(buzz.getId());
        }

        @Test
        @DisplayName("게시자가 아니면 삭제가 거절된다")
        void deleteBuzz_ByNonCreator_Denied() {
            // given
            Buzz buzz = buzz(List.of());
            given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));

            // when & then
            assertThatThrownBy(() -> buzzService.deleteBuzz(recipientId, buzz.getId()))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            verify(buzzRepository, never()).deleteById(any());
        }
    }
}
