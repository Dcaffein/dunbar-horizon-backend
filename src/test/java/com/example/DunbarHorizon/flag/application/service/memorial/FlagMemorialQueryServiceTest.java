package com.example.DunbarHorizon.flag.application.service.memorial;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.application.dto.result.MemorialResult;
import com.example.DunbarHorizon.flag.application.port.out.FlagUserPort;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FlagMemorialQueryServiceTest {

    @InjectMocks private FlagMemorialQueryService flagMemorialQueryService;

    @Mock private FlagMemorialRepository memorialRepository;
    @Mock private FlagUserPort flagUserPort;

    private static final Long FLAG_ID = 1L;
    private static final Long VIEWER_ID = 2L;
    private static final Long WRITER_ID = 3L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Test
    @DisplayName("추도문이 없으면 빈 리스트를 반환한다")
    void getMemorials_NoMemorials_ReturnsEmpty() {
        // given
        given(memorialRepository.findAllMemorialsIfMemorialized(FLAG_ID, VIEWER_ID)).willReturn(List.of());

        // when
        List<MemorialResult> result = flagMemorialQueryService.getMemorials(FLAG_ID, VIEWER_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("추도문 목록을 작성자 정보와 함께 반환한다")
    void getMemorials_WithMemorials_ReturnsResults() {
        // given
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        given(mockMemorial.getWriterId()).willReturn(WRITER_ID);
        given(mockMemorial.getId()).willReturn(1L);
        given(mockMemorial.getContent()).willReturn("추도 내용");
        given(mockMemorial.getCreatedAt()).willReturn(NOW);

        FlagUserInfo writerInfo = new FlagUserInfo(WRITER_ID, "작성자", null);

        given(memorialRepository.findAllMemorialsIfMemorialized(FLAG_ID, VIEWER_ID))
                .willReturn(List.of(mockMemorial));
        given(flagUserPort.findUserInfosByIds(any())).willReturn(Map.of(WRITER_ID, writerInfo));

        // when
        List<MemorialResult> result = flagMemorialQueryService.getMemorials(FLAG_ID, VIEWER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).writerId()).isEqualTo(WRITER_ID);
        assertThat(result.get(0).nickname()).isEqualTo("작성자");
    }

    @Test
    @DisplayName("작성자 정보가 없어도 기본 닉네임으로 추도문을 반환한다")
    void getMemorials_UnknownWriter_ReturnsDefaultNickname() {
        // given
        FlagMemorial mockMemorial = mock(FlagMemorial.class);
        given(mockMemorial.getWriterId()).willReturn(WRITER_ID);
        given(mockMemorial.getId()).willReturn(1L);
        given(mockMemorial.getContent()).willReturn("추도 내용");
        given(mockMemorial.getCreatedAt()).willReturn(NOW);

        given(memorialRepository.findAllMemorialsIfMemorialized(FLAG_ID, VIEWER_ID))
                .willReturn(List.of(mockMemorial));
        given(flagUserPort.findUserInfosByIds(any())).willReturn(Map.of()); // no writer info

        // when
        List<MemorialResult> result = flagMemorialQueryService.getMemorials(FLAG_ID, VIEWER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nickname()).isEqualTo("알 수 없는 사용자");
    }
}
