package com.example.DunbarHorizon.flag.application.service.comment;

import com.example.DunbarHorizon.flag.application.dto.result.CommentResult;
import com.example.DunbarHorizon.flag.application.port.out.FlagUserPort;
import com.example.DunbarHorizon.flag.domain.comment.FlagComment;
import com.example.DunbarHorizon.flag.domain.comment.repository.FlagCommentRepository;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FlagCommentQueryServiceTest {

    @InjectMocks private FlagCommentQueryService flagCommentQueryService;

    @Mock private FlagCommentRepository commentRepository;
    @Mock private FlagRepository flagRepository;
    @Mock private FlagUserPort flagUserPort;

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 1L;
    private static final Long VIEWER_ID = 2L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Flag createFlag() {
        FlagSchedule schedule = FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
        Flag flag = Flag.create(HOST_ID, "플래그", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    private FlagComment createComment(Long id, Long writerId, boolean isPrivate) {
        FlagComment comment = FlagComment.createRoot(FLAG_ID, writerId, "댓글 내용", isPrivate);
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "createdAt", NOW);
        return comment;
    }

    @Test
    @DisplayName("존재하지 않는 플래그의 댓글 트리를 조회하면 FlagNotFoundException이 발생한다")
    void getCommentTree_FlagNotFound_ThrowsException() {
        // given
        given(flagRepository.findById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagCommentQueryService.getCommentTree(999L, VIEWER_ID))
                .isInstanceOf(FlagNotFoundException.class);
    }

    @Test
    @DisplayName("댓글이 없는 플래그는 빈 리스트를 반환한다")
    void getCommentTree_NoComments_ReturnsEmpty() {
        // given
        Flag flag = createFlag();
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(commentRepository.findAllByFlagId(FLAG_ID)).willReturn(List.of());
        given(flagUserPort.findUserInfosByIds(anySet())).willReturn(Map.of());

        // when
        List<CommentResult> result = flagCommentQueryService.getCommentTree(FLAG_ID, VIEWER_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공개 댓글은 모든 조회자에게 반환된다")
    void getCommentTree_PublicComments_VisibleToAll() {
        // given
        Flag flag = createFlag();
        FlagComment publicComment = createComment(10L, VIEWER_ID, false);

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(commentRepository.findAllByFlagId(FLAG_ID)).willReturn(List.of(publicComment));
        given(flagUserPort.findUserInfosByIds(anySet()))
                .willReturn(Map.of(VIEWER_ID, new FlagUserInfo(VIEWER_ID, "사용자", null),
                                   HOST_ID, new FlagUserInfo(HOST_ID, "호스트", null)));

        // when
        List<CommentResult> result = flagCommentQueryService.getCommentTree(FLAG_ID, 99L); // stranger

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("비공개 댓글은 작성자가 아닌 조회자에게 필터링된다")
    void getCommentTree_PrivateComment_FilteredForStranger() {
        // given
        Flag flag = createFlag();
        FlagComment privateComment = createComment(10L, VIEWER_ID, true); // writer = VIEWER_ID

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));
        given(commentRepository.findAllByFlagId(FLAG_ID)).willReturn(List.of(privateComment));
        given(flagUserPort.findUserInfosByIds(anySet())).willReturn(Map.of());

        // when: viewed by stranger (99L)
        List<CommentResult> result = flagCommentQueryService.getCommentTree(FLAG_ID, 99L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCommentCount는 현재 0L을 반환한다")
    void getCommentCount_ReturnsZero() {
        // when
        Long count = flagCommentQueryService.getCommentCount(FLAG_ID);

        // then — Task 17 이후에 실제 카운트 로직으로 교체 예정
        assertThat(count).isEqualTo(0L);
    }
}
