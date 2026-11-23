package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.recipient.IntermediaryRecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.command.recipient.LabelRecipientSpec;
import com.example.DunbarHorizon.buzz.application.dto.command.recipient.ManualRecipientSpec;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BuzzCommandServiceTest {

    @Mock private BuzzRepository buzzRepository;
    @Mock private BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    @Mock private BuzzSocialPort buzzSocialPort;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private BuzzCommandService buzzCommandService;

    private final Long creatorId = 1L;
    private final Long recipientId = 2L;
    private final Long strangerId = 99L;

    @BeforeEach
    void setUp() {
        buzzCommandService = new BuzzCommandService(buzzRepository, buzzAudienceMemberRepository, buzzSocialPort,
                applicationEventPublisher);
    }

    private Buzz buzz(List<String> imageKeys) {
        return withId(Buzz.create(creatorId, "본문", imageKeys, Set.of(recipientId), LocalDateTime.now()), 1L);
    }

    @Nested
    @DisplayName("버즈 생성")
    class CreateBuzz {

        @BeforeEach
        void setUp() {
            given(buzzSocialPort.getFriendIdsIn(creatorId, List.of(recipientId, 3L))).willReturn(Set.of(recipientId, 3L));
        }

        @Test
        @DisplayName("이미지 키를 서명하지 않고 그대로 저장한다")
        void createBuzz_SavesImageKeys() {
            // when
            buzzCommandService.createBuzz(command(), List.of("buzz/photo"));

            // then
            ArgumentCaptor<Buzz> buzzCaptor = ArgumentCaptor.forClass(Buzz.class);
            verify(buzzRepository).save(buzzCaptor.capture());
            assertThat(buzzCaptor.getValue().getImageKeys()).containsExactly("buzz/photo");
        }

        @Test
        @DisplayName("지정 결과의 사용자마다 미읽음 공개 대상 멤버를 저장하고 생성 이벤트를 발행한다")
        @SuppressWarnings("unchecked")
        void createBuzz_SavesAudienceMembersAndPublishesEvent() {
            // when
            buzzCommandService.createBuzz(command(), List.of());

            // then
            ArgumentCaptor<Buzz> buzzCaptor = ArgumentCaptor.forClass(Buzz.class);
            verify(buzzRepository).save(buzzCaptor.capture());
            Long buzzId = buzzCaptor.getValue().getId();

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
    @DisplayName("공개 대상 지정")
    class AudienceSpec {

        @Test
        @DisplayName("직접 지정하면 게시자 본인을 빼고 게시자의 친구만 공개 대상이 된다")
        void createBuzz_Manual_ExcludesCreatorAndNonFriends() {
            // given
            given(buzzSocialPort.getFriendIdsIn(creatorId, List.of(recipientId, 5L))).willReturn(Set.of(recipientId));

            // when
            buzzCommandService.createBuzz(new CreateBuzzCommand(creatorId, "텍스트",
                    new ManualRecipientSpec(List.of(creatorId, recipientId, 5L))), List.of());

            // then
            verify(applicationEventPublisher).publishEvent(any(BuzzCreatedEvent.class));
            verify(buzzSocialPort).getFriendIdsIn(creatorId, List.of(recipientId, 5L));
        }

        @Test
        @DisplayName("라벨로 지정하면 게시자 라벨의 멤버가 공개 대상이 된다")
        void createBuzz_Label_UsesLabelMembers() {
            // given
            given(buzzSocialPort.getLabelMemberIds(creatorId, List.of("label-1"))).willReturn(Set.of(recipientId));

            // when
            buzzCommandService.createBuzz(new CreateBuzzCommand(creatorId, "텍스트",
                    new LabelRecipientSpec(List.of("label-1"))), List.of());

            // then
            ArgumentCaptor<BuzzCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BuzzCreatedEvent.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().recipientIds()).containsExactly(recipientId);
        }

        @Test
        @DisplayName("중개 친구로 지정하면 중개 친구를 통한 확장 결과가 공개 대상이 된다")
        void createBuzz_Intermediary_UsesExpansion() {
            // given
            given(buzzSocialPort.getIntermediaryExpansionIds(creatorId, 7L, 0.5)).willReturn(Set.of(recipientId, 8L));

            // when
            buzzCommandService.createBuzz(new CreateBuzzCommand(creatorId, "텍스트",
                    new IntermediaryRecipientSpec(7L, 0.5)), List.of());

            // then
            ArgumentCaptor<BuzzCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BuzzCreatedEvent.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().recipientIds()).containsExactlyInAnyOrder(recipientId, 8L);
        }
    }

    @Nested
    @DisplayName("버즈 읽기")
    class ReadBuzz {

        private Buzz buzz;

        @BeforeEach
        void setUp() {
            buzz = buzz(List.of());
            given(buzzRepository.findByIdForShare(buzz.getId())).willReturn(Optional.of(buzz));
        }

        @Test
        @DisplayName("공개 대상 멤버가 버즈를 읽으면 그 멤버가 읽음으로 바뀐다")
        void readBuzz_Member_Reads() {
            // given
            BuzzAudienceMember member = BuzzAudienceMember.create(buzz.getId(), recipientId);
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), recipientId))
                    .willReturn(Optional.of(member));

            // when
            buzzCommandService.readBuzz(recipientId, buzz.getId());

            // then
            assertThat(member.isUnread()).isFalse();
            verify(buzzAudienceMemberRepository).save(member);
        }

        @Test
        @DisplayName("게시자가 읽으면 멤버를 찾지 않고 아무것도 바꾸지 않는다")
        void readBuzz_Creator_NoOp() {
            // when
            buzzCommandService.readBuzz(creatorId, buzz.getId());

            // then
            verify(buzzAudienceMemberRepository, never()).findByBuzzIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("게시자도 공개 대상 멤버도 아니면 읽기가 거절된다")
        void readBuzz_Stranger_Denied() {
            given(buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), strangerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> buzzCommandService.readBuzz(strangerId, buzz.getId()))
                    .isInstanceOf(BuzzAccessDeniedException.class);
        }
    }

    @Test
    @DisplayName("없거나 삭제된 버즈는 읽을 수 없다")
    void readBuzz_Missing_NotFound() {
        given(buzzRepository.findByIdForShare(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> buzzCommandService.readBuzz(recipientId, 999L))
                .isInstanceOf(BuzzNotFoundException.class);
    }

    @Nested
    @DisplayName("버즈 수정")
    class UpdateBuzz {

        @Test
        @DisplayName("게시자가 수정하면 본문·이미지 키가 바뀌고 수정 시각이 남는다")
        void updateBuzz_ByCreator_ShowsEditedAt() {
            // given
            Buzz buzz = buzz(List.of("buzz/a"));
            given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));

            // when
            buzzCommandService.updateBuzz(new UpdateBuzzCommand(creatorId, buzz.getId(), "고친 본문", List.of("buzz/b")));

            // then
            assertThat(buzz.getText()).isEqualTo("고친 본문");
            assertThat(buzz.getImageKeys()).containsExactly("buzz/b");
            assertThat(buzz.getEditedAt()).isNotNull();
            verify(buzzRepository).save(buzz);
        }

        @Test
        @DisplayName("게시자가 아니면 수정이 거절된다")
        void updateBuzz_ByNonCreator_Denied() {
            // given
            Buzz buzz = buzz(List.of());
            given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));

            // when & then
            assertThatThrownBy(() -> buzzCommandService.updateBuzz(
                    new UpdateBuzzCommand(recipientId, buzz.getId(), "고친 본문", List.of())))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            assertThat(buzz.getText()).isEqualTo("본문");
        }

        @Test
        @DisplayName("없는 버즈는 수정할 수 없다")
        void updateBuzz_Missing_NotFound() {
            given(buzzRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> buzzCommandService.updateBuzz(
                    new UpdateBuzzCommand(creatorId, 999L, "고친 본문", List.of())))
                    .isInstanceOf(BuzzNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("버즈 삭제")
    class DeleteBuzz {

        @Test
        @DisplayName("게시자가 삭제하면 삭제 표시만 남기고 자식은 지우지 않는다")
        void deleteBuzz_ByCreator_MarksDeletedOnly() {
            // given
            Buzz buzz = buzz(List.of());
            given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));

            // when
            buzzCommandService.deleteBuzz(creatorId, buzz.getId());

            // then
            assertThat(buzz.getDeletedAt()).isNotNull();
            verify(buzzRepository).save(buzz);
            verifyNoInteractions(buzzAudienceMemberRepository);
        }

        @Test
        @DisplayName("게시자가 아니면 삭제가 거절된다")
        void deleteBuzz_ByNonCreator_Denied() {
            // given
            Buzz buzz = buzz(List.of());
            given(buzzRepository.findById(buzz.getId())).willReturn(Optional.of(buzz));

            // when & then
            assertThatThrownBy(() -> buzzCommandService.deleteBuzz(recipientId, buzz.getId()))
                    .isInstanceOf(BuzzAccessDeniedException.class);
            assertThat(buzz.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("없거나 이미 삭제된 버즈는 찾을 수 없다")
        void deleteBuzz_Missing_NotFound() {
            given(buzzRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> buzzCommandService.deleteBuzz(creatorId, 999L))
                    .isInstanceOf(BuzzNotFoundException.class);
        }
    }

    private static Buzz withId(Buzz buzz, long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(buzz, "id", id);
        return buzz;
    }
}
