package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.WriteBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.service.BuzzThreadAccess.ThreadContext;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCommentedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzCommentNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzCommentRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class BuzzCommentService {

    private final BuzzRepository buzzRepository;
    private final BuzzThreadRepository buzzThreadRepository;
    private final BuzzCommentRepository buzzCommentRepository;
    private final BuzzThreadAccess buzzThreadAccess;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Long writeComment(WriteBuzzCommentCommand command) {
        LocalDateTime now = LocalDateTime.now();
        ThreadContext context = buzzThreadAccess.forWriting(
                command.authorId(), command.buzzId(), command.threadId(), now);

        BuzzComment comment = buzzCommentRepository.save(BuzzComment.write(
                context.thread().getId(), command.authorId(), command.text(), command.imageKeys(), now));
        context.member().ifPresent(BuzzAudienceMember::markAsRead);

        applicationEventPublisher.publishEvent(new BuzzCommentedEvent(
                context.buzz().getId(), context.thread().getId(), context.buzz().getCreatorId(),
                context.thread().getStarterId(), command.authorId()));
        return comment.getId();
    }

    public void updateComment(UpdateBuzzCommentCommand command) {
        Buzz buzz = buzzOf(command.buzzId());
        BuzzThread thread = threadOf(buzz, command.threadId());
        BuzzComment comment = commentOf(thread, command.commentId());

        comment.update(command.requesterId(), command.text(), command.imageKeys());
    }

    public void deleteComment(Long requesterId, String buzzId, Long threadId, Long commentId) {
        Buzz buzz = buzzOf(buzzId);
        BuzzThread thread = threadOf(buzz, threadId);
        BuzzComment comment = commentOf(thread, commentId);
        comment.validateDeletion(requesterId, buzz.getCreatorId());

        buzzCommentRepository.deleteById(commentId);
    }

    private Buzz buzzOf(String buzzId) {
        return buzzRepository.findById(buzzId)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
    }

    private BuzzThread threadOf(Buzz buzz, Long threadId) {
        return buzzThreadRepository.findById(threadId)
                .filter(thread -> thread.getBuzzId().equals(buzz.getId()))
                .orElseThrow(() -> new BuzzThreadNotFoundException(buzz.getId(), threadId));
    }

    private BuzzComment commentOf(BuzzThread thread, Long commentId) {
        return buzzCommentRepository.findById(commentId)
                .filter(comment -> comment.getThreadId().equals(thread.getId()))
                .orElseThrow(() -> new BuzzCommentNotFoundException(thread.getId(), commentId));
    }
}
