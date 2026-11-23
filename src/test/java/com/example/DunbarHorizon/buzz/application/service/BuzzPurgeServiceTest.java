package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.port.out.BuzzPurgeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuzzPurgeServiceTest {

    @Mock private BuzzPurgeRepository buzzPurgeRepository;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks private BuzzPurgeService buzzPurgeService;

    @Test
    @DisplayName("수명 기준 시각으로 정리 대상을 골라 정리하고 정리한 버즈 수를 돌려준다")
    void purge_PurgesSelectedBuzzes() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<Long> buzzIds = List.of(1L, 2L);
        given(buzzPurgeRepository.findPurgeableIds(now.minusHours(6), BuzzPurgeService.BATCH_SIZE)).willReturn(buzzIds);

        // when
        int purged = buzzPurgeService.purgeExpiredAndDeletedBuzzes(now);

        // then
        assertThat(purged).isEqualTo(2);
        verify(buzzPurgeRepository).purge(buzzIds);
    }

    @Test
    @DisplayName("한 번에 상한만큼 정리되면 상한보다 적게 남을 때까지 다음 묶음을 정리한다")
    void purge_FullBatch_ContinuesUntilPartialBatch() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<Long> fullBatch = LongStream.rangeClosed(1, BuzzPurgeService.BATCH_SIZE).boxed().toList();
        List<Long> lastBatch = List.of(5_000L);
        given(buzzPurgeRepository.findPurgeableIds(now.minusHours(6), BuzzPurgeService.BATCH_SIZE))
                .willReturn(fullBatch, lastBatch);

        // when
        int purged = buzzPurgeService.purgeExpiredAndDeletedBuzzes(now);

        // then
        assertThat(purged).isEqualTo(BuzzPurgeService.BATCH_SIZE + 1);
        verify(buzzPurgeRepository).purge(fullBatch);
        verify(buzzPurgeRepository).purge(lastBatch);
        verify(transactionManager, times(2)).commit(any());
    }

    @Test
    @DisplayName("정리할 버즈가 없으면 아무것도 지우지 않는다")
    void purge_Nothing() {
        // given
        LocalDateTime now = LocalDateTime.now();
        given(buzzPurgeRepository.findPurgeableIds(now.minusHours(6), BuzzPurgeService.BATCH_SIZE)).willReturn(List.of());

        // when
        int purged = buzzPurgeService.purgeExpiredAndDeletedBuzzes(now);

        // then
        assertThat(purged).isZero();
        verify(buzzPurgeRepository, never()).purge(any());
    }
}
