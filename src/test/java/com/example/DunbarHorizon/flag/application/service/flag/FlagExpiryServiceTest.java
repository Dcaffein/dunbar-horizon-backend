package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagExpiryServiceTest {

    @InjectMocks private FlagExpiryService flagExpiryService;
    @Mock private FlagRepository flagRepository;

    @Test
    @DisplayName("만료 임계값이 현재 시각 기준 24시간 전으로 계산된다")
    void labelExpiredFlags_ThresholdIs24HoursBefore() {
        // given
        given(flagRepository.expireAllExceedingThreshold(any())).willReturn(0);
        LocalDateTime before = LocalDateTime.now().minusHours(Flag.EXPIRATION_THRESHOLD_HOURS);

        // when
        flagExpiryService.labelExpiredFlags();

        // then
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(flagRepository).expireAllExceedingThreshold(captor.capture());

        LocalDateTime threshold = captor.getValue();
        assertThat(threshold).isBeforeOrEqualTo(before.plusSeconds(1));
        assertThat(threshold).isAfterOrEqualTo(before.minusSeconds(1));
    }

    @Test
    @DisplayName("만료된 플래그가 있으면 expireAllExceedingThreshold가 호출된다")
    void labelExpiredFlags_CallsRepository() {
        // given
        given(flagRepository.expireAllExceedingThreshold(any())).willReturn(3);

        // when
        flagExpiryService.labelExpiredFlags();

        // then
        verify(flagRepository).expireAllExceedingThreshold(any(LocalDateTime.class));
    }
}
