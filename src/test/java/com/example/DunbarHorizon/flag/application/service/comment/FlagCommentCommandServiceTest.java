package com.example.DunbarHorizon.flag.application.service.comment;

import com.example.DunbarHorizon.flag.domain.comment.FlagComment;
import com.example.DunbarHorizon.flag.domain.comment.exception.FlagCommentAuthorizationException;
import com.example.DunbarHorizon.flag.domain.comment.exception.FlagCommentNotFoundException;
import com.example.DunbarHorizon.flag.domain.comment.exception.FlagCommentReplyDepthException;
import com.example.DunbarHorizon.flag.domain.comment.repository.FlagCommentRepository;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlagCommentCommandServiceTest {

    @InjectMocks private FlagCommentCommandService flagCommentCommandService;

    @Mock private FlagCommentRepository commentRepository;
    @Mock private FlagRepository flagRepository;
    @Mock private FlagParticipantRepository participantRepository;

    private static final Long FLAG_ID = 1L;
    private static final Long HOST_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Flag createFlag() {
        FlagSchedule schedule = FlagSchedule.of(NOW.plusHours(2), NOW.plusHours(3), NOW.plusHours(4));
        Flag flag = Flag.create(HOST_ID, "플래그", "설명", 10, schedule);
        ReflectionTestUtils.setField(flag, "id", FLAG_ID);
        return flag;
    }

    @Test
    @DisplayName("정상적으로 루트 댓글을 생성할 수 있다")
    void createRootComment_Success() {
        // given
        FlagComment saved = FlagComment.createRoot(FLAG_ID, USER_ID, "댓글 내용", false);
        ReflectionTestUtils.setField(saved, "id", 10L);

        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(createFlag()));
        given(participantRepository.findAllParticipantIdsByFlagId(FLAG_ID)).willReturn(List.of(USER_ID));
        given(commentRepository.save(any(FlagComment.class))).willReturn(saved);

        // when
        Long result = flagCommentCommandService.createRootComment(FLAG_ID, USER_ID, "댓글 내용", false);

        // then
        assertThat(result).isEqualTo(10L);
        verify(commentRepository).save(any(FlagComment.class));
    }

    @Test
    @DisplayName("존재하지 않는 플래그에 댓글을 달면 FlagNotFoundException이 발생한다")
    void createRootComment_FlagNotFound_ThrowsException() {
        // given
        given(flagRepository.findById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagCommentCommandService.createRootComment(999L, USER_ID, "내용", false))
                .isInstanceOf(FlagNotFoundException.class);
    }

    @Test
    @DisplayName("부모 댓글에 정상적으로 답글을 생성할 수 있다")
    void createReply_Success() {
        // given
        FlagComment parent = FlagComment.createRoot(FLAG_ID, HOST_ID, "루트 댓글", false);
        ReflectionTestUtils.setField(parent, "id", 1L);

        FlagComment savedReply = parent.createReply(USER_ID, "답글 내용", false);
        ReflectionTestUtils.setField(savedReply, "id", 2L);

        given(commentRepository.findById(1L)).willReturn(Optional.of(parent));
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(createFlag()));
        given(participantRepository.findAllParticipantIdsByFlagId(FLAG_ID)).willReturn(List.of(USER_ID));
        given(commentRepository.save(any(FlagComment.class))).willReturn(savedReply);

        // when
        Long result = flagCommentCommandService.createReply(1L, USER_ID, "답글 내용", false);

        // then
        assertThat(result).isEqualTo(2L);
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 답글을 달면 FlagCommentNotFoundException이 발생한다")
    void createReply_ParentNotFound_ThrowsException() {
        // given
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagCommentCommandService.createReply(999L, USER_ID, "내용", false))
                .isInstanceOf(FlagCommentNotFoundException.class);
    }

    @Test
    @DisplayName("답글에 답글을 달면 FlagCommentReplyDepthException이 발생한다")
    void createReply_ToReply_ThrowsException() {
        // given: parent is already a reply (has parentId)
        FlagComment root = FlagComment.createRoot(FLAG_ID, HOST_ID, "루트", false);
        ReflectionTestUtils.setField(root, "id", 1L);
        FlagComment reply = root.createReply(HOST_ID, "1단 답글", false);
        ReflectionTestUtils.setField(reply, "id", 2L);

        given(commentRepository.findById(2L)).willReturn(Optional.of(reply));
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(createFlag()));
        given(participantRepository.findAllParticipantIdsByFlagId(FLAG_ID)).willReturn(List.of(USER_ID));

        // when / then
        assertThatThrownBy(() -> flagCommentCommandService.createReply(2L, USER_ID, "2단 답글", false))
                .isInstanceOf(FlagCommentReplyDepthException.class);
    }

    @Test
    @DisplayName("작성자가 댓글을 수정할 수 있다")
    void updateComment_ByWriter_Success() {
        // given
        FlagComment comment = FlagComment.createRoot(FLAG_ID, USER_ID, "원래 내용", false);
        ReflectionTestUtils.setField(comment, "id", 1L);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        // when
        flagCommentCommandService.updateComment(1L, USER_ID, "수정된 내용", false);

        // then
        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 댓글을 수정하면 FlagCommentAuthorizationException이 발생한다")
    void updateComment_ByNonWriter_ThrowsException() {
        // given
        FlagComment comment = FlagComment.createRoot(FLAG_ID, USER_ID, "내용", false);
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        // when / then
        assertThatThrownBy(() -> flagCommentCommandService.updateComment(1L, HOST_ID, "수정 시도", false))
                .isInstanceOf(FlagCommentAuthorizationException.class);
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 수정하면 FlagCommentNotFoundException이 발생한다")
    void updateComment_NotFound_ThrowsException() {
        // given
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> flagCommentCommandService.updateComment(999L, USER_ID, "내용", false))
                .isInstanceOf(FlagCommentNotFoundException.class);
    }

    @Test
    @DisplayName("작성자가 댓글을 삭제할 수 있다")
    void deleteComment_ByWriter_Success() {
        // given
        FlagComment comment = FlagComment.createRoot(FLAG_ID, USER_ID, "내용", false);
        ReflectionTestUtils.setField(comment, "id", 1L);
        Flag flag = createFlag();

        given(commentRepository.findByIdForUpdate(1L)).willReturn(Optional.of(comment));
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));

        // when
        flagCommentCommandService.deleteComment(1L, USER_ID);

        // then
        verify(commentRepository).deleteWithReplies(1L);
    }

    @Test
    @DisplayName("작성자도 호스트도 아닌 사용자가 댓글을 삭제하면 FlagCommentAuthorizationException이 발생한다")
    void deleteComment_ByStranger_ThrowsException() {
        // given
        FlagComment comment = FlagComment.createRoot(FLAG_ID, USER_ID, "내용", false);
        ReflectionTestUtils.setField(comment, "id", 1L);
        Flag flag = createFlag();

        given(commentRepository.findByIdForUpdate(1L)).willReturn(Optional.of(comment));
        given(flagRepository.findById(FLAG_ID)).willReturn(Optional.of(flag));

        // when / then
        assertThatThrownBy(() -> flagCommentCommandService.deleteComment(1L, 99L))
                .isInstanceOf(FlagCommentAuthorizationException.class);
    }
}
