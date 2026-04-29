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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("버즈 생성 시 이미지 업로드")
    class CreateBuzzImageUpload {

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
        @DisplayName("이미지 파일이 있으면 S3에 업로드하고 반환된 URL을 Buzz에 저장한다")
        void createBuzz_WithImages_UploadsAndSavesUrls() {
            // given
            MultipartFile file = new MockMultipartFile("images", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
            List<String> expectedUrls = List.of("https://bucket.s3.ap-northeast-2.amazonaws.com/uuid_photo.jpg");
            given(imageStoragePort.upload(anyList())).willReturn(expectedUrls);

            // when
            buzzService.createBuzz(command, List.of(file));

            // then
            ArgumentCaptor<Buzz> buzzCaptor = ArgumentCaptor.forClass(Buzz.class);
            verify(buzzRepository).save(buzzCaptor.capture());
            assertThat(buzzCaptor.getValue().getImageUrls()).isEqualTo(expectedUrls);
        }

        @Test
        @DisplayName("이미지 파일이 없으면 imageUrls가 빈 리스트로 저장된다")
        void createBuzz_WithoutImages_SavesEmptyUrls() {
            // given
            given(imageStoragePort.upload(any())).willReturn(List.of());

            // when
            buzzService.createBuzz(command, List.of());

            // then
            ArgumentCaptor<Buzz> buzzCaptor = ArgumentCaptor.forClass(Buzz.class);
            verify(buzzRepository).save(buzzCaptor.capture());
            assertThat(buzzCaptor.getValue().getImageUrls()).isEmpty();
        }

        @Test
        @DisplayName("images가 null이면 imageStoragePort.upload에 null이 전달된다")
        void createBuzz_NullImages_UploadCalledWithNull() {
            // given
            given(imageStoragePort.upload(null)).willReturn(List.of());

            // when
            buzzService.createBuzz(command, null);

            // then
            verify(imageStoragePort).upload(null);
        }
    }
}
