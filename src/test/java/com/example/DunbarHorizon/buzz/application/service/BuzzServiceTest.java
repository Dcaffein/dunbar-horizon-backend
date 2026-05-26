package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;
import com.example.DunbarHorizon.buzz.application.dto.info.recipient.ManualRecipientSpec;
import com.example.DunbarHorizon.buzz.application.port.in.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuzzServiceTest {

    @Mock private RecipientStrategyProvider strategyProvider;
    @Mock private BuzzRepository buzzRepository;
    @Mock private BuzzSocialPort buzzSocialPort;
    @Mock private ImageStoragePort imageStoragePort;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private RecipientStrategyPort recipientStrategyPort;

    private BuzzService buzzService;

    private final Long creatorId = 1L;
    private final Long recipientId = 2L;

    @BeforeEach
    void setUp() {
        buzzService = new BuzzService(
                strategyProvider, buzzRepository, buzzSocialPort, imageStoragePort, eventPublisher);
    }

    @Nested
    @DisplayName("버즈 생성 시 이미지 URL 변환")
    class CreateBuzzImageResolve {

        private CreateBuzzCommand command;

        @BeforeEach
        void setUp() {
            command = new CreateBuzzCommand(creatorId, "텍스트", new ManualRecipientSpec(List.of(recipientId)));

            given(buzzSocialPort.getCreatorProfiles(any()))
                    .willReturn(List.of(new BuzzCreatorInfo(creatorId, "작성자", "profile.png")));
            given(strategyProvider.getStrategy(RecipientType.MANUAL)).willReturn(recipientStrategyPort);
            given(recipientStrategyPort.fetchRecipientIds(any(), any())).willReturn(Set.of(recipientId));
            given(buzzRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("imageKeys가 있으면 presigned GET URL로 변환하여 Buzz에 저장한다")
        void createBuzz_WithImageKeys_ResolvesAndSavesUrls() {
            List<String> imageKeys = List.of("buzz/uuid-photo");
            List<String> expectedUrls = List.of("https://bucket.s3.amazonaws.com/buzz/uuid-photo?X-Amz-Signature=abc");
            given(imageStoragePort.resolveUrls(imageKeys)).willReturn(expectedUrls);

            buzzService.createBuzz(command, imageKeys);

            ArgumentCaptor<Buzz> buzzCaptor = ArgumentCaptor.forClass(Buzz.class);
            verify(buzzRepository).save(buzzCaptor.capture());
            assertThat(buzzCaptor.getValue().getImageUrls()).isEqualTo(expectedUrls);
        }

        @Test
        @DisplayName("imageKeys가 없으면 imageUrls가 빈 리스트로 저장된다")
        void createBuzz_WithoutImageKeys_SavesEmptyUrls() {
            given(imageStoragePort.resolveUrls(List.of())).willReturn(List.of());

            buzzService.createBuzz(command, List.of());

            ArgumentCaptor<Buzz> buzzCaptor = ArgumentCaptor.forClass(Buzz.class);
            verify(buzzRepository).save(buzzCaptor.capture());
            assertThat(buzzCaptor.getValue().getImageUrls()).isEmpty();
        }
    }

    @Nested
    @DisplayName("만료된 버즈 댓글 작성 시 resolveUrls가 호출되지 않는다")
    class CommentOnExpiredBuzz {

        private Buzz expiredBuzz;

        @BeforeEach
        void setUp() {
            expiredBuzz = Buzz.builder()
                    .creatorId(creatorId)
                    .creatorNickname("작성자")
                    .creatorProfileImageUrl("p.png")
                    .text("Buzz")
                    .recipientIds(List.of(recipientId))
                    .build();
            ReflectionTestUtils.setField(expiredBuzz, "expiresAt", expiredBuzz.getCreatedAt().minusMinutes(1));
            ReflectionTestUtils.setField(expiredBuzz, "id", "buzz-id");

            given(buzzRepository.findById("buzz-id")).willReturn(expiredBuzz);
            given(buzzSocialPort.getCreatorProfiles(any()))
                    .willReturn(List.of(new BuzzCreatorInfo(recipientId, "수신자", "p.png")));
        }

        @Test
        @DisplayName("만료된 버즈에 댓글을 달면 resolveUrls 호출 없이 예외가 발생한다")
        void commentOnExpiredBuzz_ThrowsWithoutResolve() {
            assertThatThrownBy(() ->
                    buzzService.commentOnBuzz(recipientId, "buzz-id", "댓글", List.of(), true))
                    .isInstanceOf(BuzzInvalidStateException.class);

            verify(imageStoragePort, never()).resolveUrls(anyList());
        }
    }
}
