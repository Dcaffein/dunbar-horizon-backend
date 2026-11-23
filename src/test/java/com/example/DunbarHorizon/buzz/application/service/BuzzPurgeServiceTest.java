package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzCommentRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuzzPurgeServiceTest {

    @Mock private BuzzRepository buzzRepository;
    @Mock private BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    @Mock private BuzzThreadRepository buzzThreadRepository;
    @Mock private BuzzCommentRepository buzzCommentRepository;

    @InjectMocks private BuzzPurgeService buzzPurgeService;

    @Test
    @DisplayName("버즈 행을 먼저 지우고 멤버와 스레드를 지운 뒤 미리 읽어 둔 스레드 ID로 답글을 지운다")
    void purge_DeletesBuzzesThenChildren() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<Long> buzzIds = List.of(1L, 2L);
        given(buzzRepository.findPurgeableIds(now.minusHours(6))).willReturn(buzzIds);
        given(buzzThreadRepository.findIdsByBuzzIds(buzzIds)).willReturn(List.of(10L, 11L));

        // when
        int purged = buzzPurgeService.purgeExpiredAndDeletedBuzzes(now);

        // then
        assertThat(purged).isEqualTo(2);
        InOrder order = inOrder(buzzRepository, buzzThreadRepository, buzzAudienceMemberRepository, buzzCommentRepository);
        order.verify(buzzRepository).deleteAllByIds(buzzIds);
        order.verify(buzzThreadRepository).findIdsByBuzzIds(buzzIds);
        order.verify(buzzAudienceMemberRepository).deleteAllByBuzzIds(buzzIds);
        order.verify(buzzThreadRepository).deleteAllByBuzzIds(buzzIds);
        order.verify(buzzCommentRepository).deleteAllByThreadIds(List.of(10L, 11L));
    }

    @Test
    @DisplayName("스레드가 없으면 답글 삭제를 건너뛴다")
    void purge_NoThreads_SkipsComments() {
        // given
        LocalDateTime now = LocalDateTime.now();
        given(buzzRepository.findPurgeableIds(now.minusHours(6))).willReturn(List.of(1L));
        given(buzzThreadRepository.findIdsByBuzzIds(List.of(1L))).willReturn(List.of());

        // when
        buzzPurgeService.purgeExpiredAndDeletedBuzzes(now);

        // then
        verify(buzzCommentRepository, never()).deleteAllByThreadIds(any());
    }

    @Test
    @DisplayName("정리할 버즈가 없으면 아무것도 지우지 않는다")
    void purge_Nothing() {
        // given
        LocalDateTime now = LocalDateTime.now();
        given(buzzRepository.findPurgeableIds(now.minusHours(6))).willReturn(List.of());

        // when
        int purged = buzzPurgeService.purgeExpiredAndDeletedBuzzes(now);

        // then
        assertThat(purged).isZero();
        verify(buzzRepository, never()).deleteAllByIds(any());
    }
}
