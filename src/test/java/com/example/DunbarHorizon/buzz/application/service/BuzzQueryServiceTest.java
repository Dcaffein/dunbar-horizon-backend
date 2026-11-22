package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BuzzQueryServiceTest {

    @Mock private BuzzQueryRepository buzzQueryRepository;
    @Mock private BuzzSocialPort buzzSocialPort;
    @Mock private ImageStoragePort imageStoragePort;

    private BuzzQueryService buzzQueryService;

    private final Long creatorId = 1L;
    private final Long recipientId = 2L;

    @BeforeEach
    void setUp() {
        buzzQueryService = new BuzzQueryService(buzzQueryRepository, buzzSocialPort,
                new BuzzProfileAssembler(buzzSocialPort), imageStoragePort);
    }

    private Buzz buzz() {
        Buzz buzz = Buzz.create(creatorId, "본문", List.of(), Set.of(recipientId), LocalDateTime.now());
        org.springframework.test.util.ReflectionTestUtils.setField(buzz, "id", 1L);
        return buzz;
    }

    @Test
    @DisplayName("받은 목록은 멤버의 읽음 상태와 응답 시점의 작성자 프로필로 만든다")
    void getReceivedBuzzes_MapsReadStateAndProfile() {
        // given
        Buzz buzz = buzz();
        PageRequest pageable = PageRequest.of(0, 20);
        given(buzzSocialPort.getNonReceivableFriendIds(recipientId)).willReturn(Set.of());
        given(buzzQueryRepository.findReceived(eq(recipientId), eq(Set.of()), any(), eq(pageable)))
                .willReturn(new SliceImpl<>(List.of(new ReceivedBuzz(buzz, false)), pageable, false));
        given(buzzSocialPort.getCreatorProfiles(List.of(creatorId)))
                .willReturn(List.of(new BuzzCreatorInfo(creatorId, "작성자", "signed-creator")));

        // when
        Slice<BuzzSummaryResult> result = buzzQueryService.getReceivedBuzzes(recipientId, pageable);

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
        Buzz buzz = buzz();
        PageRequest pageable = PageRequest.of(0, 20);
        given(buzzQueryRepository.findSent(eq(creatorId), any(), eq(pageable)))
                .willReturn(new SliceImpl<>(List.of(buzz), pageable, false));
        given(buzzSocialPort.getCreatorProfiles(List.of(creatorId)))
                .willReturn(List.of(new BuzzCreatorInfo(creatorId, "작성자", null)));

        // when
        Slice<BuzzSummaryResult> result = buzzQueryService.getSentBuzzes(creatorId, pageable);

        // then
        assertThat(result.getContent().get(0).isCreator()).isTrue();
        assertThat(result.getContent().get(0).isUnread()).isFalse();
    }
}
