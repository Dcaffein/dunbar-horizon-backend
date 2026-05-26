package com.example.DunbarHorizon.flag.application.service.comment;

import com.example.DunbarHorizon.flag.application.port.in.FlagCommentCommandUseCase;
import com.example.DunbarHorizon.flag.domain.comment.FlagComment;
import com.example.DunbarHorizon.flag.domain.comment.exception.FlagCommentNotFoundException;
import com.example.DunbarHorizon.flag.domain.comment.repository.FlagCommentRepository;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlagCommentCommandService implements FlagCommentCommandUseCase {

    private final FlagCommentRepository commentRepository;
    private final FlagRepository flagRepository;
    private final FlagParticipantRepository participantRepository;

    @Override
    @Transactional
    public Long createRootComment(Long flagId, Long userId, String content, boolean isPrivate) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        List<Long> participantIds = participantRepository.findAllParticipantIdsByFlagId(flagId);
        flag.validateCommentCreation(userId, participantIds);

        FlagComment comment = FlagComment.createRoot(flagId, userId, content, isPrivate);
        return commentRepository.save(comment).getId();
    }

    @Override
    @Transactional
    public Long createReply(Long parentId, Long userId, String content, boolean isPrivate) {
        FlagComment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new FlagCommentNotFoundException(parentId));

        Flag flag = flagRepository.findById(parent.getFlagId())
                .orElseThrow(() -> new FlagNotFoundException(parent.getFlagId()));
        List<Long> participantIds = participantRepository.findAllParticipantIdsByFlagId(parent.getFlagId());
        flag.validateCommentCreation(userId, participantIds);

        FlagComment reply = parent.createReply(userId, content, isPrivate);
        return commentRepository.save(reply).getId();
    }

    @Override
    @Transactional
    public void updateComment(Long commentId, Long userId, String content, boolean isPrivate) {
        FlagComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new FlagCommentNotFoundException(commentId));

        comment.update(userId, content, isPrivate);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        FlagComment comment = commentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new FlagCommentNotFoundException(commentId));

        Flag flag = flagRepository.findById(comment.getFlagId())
                .orElseThrow(() -> new FlagNotFoundException(comment.getFlagId()));

        comment.validateDeletionAuthority(userId, flag.getHostId());

        commentRepository.deleteWithReplies(comment.getId());
    }
}