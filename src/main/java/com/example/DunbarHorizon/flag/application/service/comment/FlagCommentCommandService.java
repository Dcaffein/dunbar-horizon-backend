package com.example.DunbarHorizon.flag.application.service.comment;

import com.example.DunbarHorizon.flag.application.port.in.FlagCommentCommandUseCase;
import com.example.DunbarHorizon.flag.domain.comment.CommentDeletionScope;
import com.example.DunbarHorizon.flag.domain.comment.FlagComment;
import com.example.DunbarHorizon.flag.domain.comment.exception.FlagCommentNotFoundException;
import com.example.DunbarHorizon.flag.domain.comment.repository.FlagCommentRepository;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FlagCommentCommandService implements FlagCommentCommandUseCase {

    private final FlagCommentRepository commentRepository;
    private final FlagRepository flagRepository;

    @Override
    public Long createRootComment(Long flagId, Long userId, String content, boolean isPrivate) {
        if(!flagRepository.existsById(flagId)){
            throw new FlagNotFoundException(flagId);
        }
        FlagComment comment = FlagComment.createRoot(flagId, userId, content, isPrivate);
        return commentRepository.save(comment).getId();
    }

    @Override
    public Long createReply(Long parentId, Long userId, String content, boolean isPrivate) {

        FlagComment parent = commentRepository.findByIdForUpdate(parentId)
                .orElseThrow(() -> new FlagNotFoundException(parentId));

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

        Flag flag = flagRepository.findById(comment.getFlagId()).orElseThrow();

        CommentDeletionScope scope = comment.issueDeletionScope(userId, flag.getHostId());

        commentRepository.delete(scope);
    }
}