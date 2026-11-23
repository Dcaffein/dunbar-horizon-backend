package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.WriteBuzzCommentCommand;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCommentedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzCommentNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzCommentRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BuzzCommentCommandService {

    private final BuzzRepository buzzRepository;
    private final BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    private final BuzzThreadRepository buzzThreadRepository;
    private final BuzzCommentRepository buzzCommentRepository;
    private final BuzzSocialPort buzzSocialPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Long writeComment(WriteBuzzCommentCommand command) {
        Buzz buzz = buzzRepository.findByIdForShare(command.buzzId())
                .orElseThrow(() -> new BuzzNotFoundException(command.buzzId()));
        boolean isAudienceMember = isAudienceMember(buzz, command.authorId());
        buzz.validateAccess(command.authorId(), isAudienceMember);

        BuzzThread thread = buzzThreadRepository.findByIdForShare(command.threadId())
                .filter(found -> found.getBuzzId().equals(buzz.getId()))
                .orElseThrow(() -> new BuzzThreadNotFoundException(buzz.getId(), command.threadId()));
        thread.validateAccess(command.authorId(), buzz, isFriendOfStarter(command.authorId(), buzz, thread));

        BuzzComment comment = buzzCommentRepository.save(BuzzComment.write(
                thread, command.authorId(), command.text(), command.imageKeys(), LocalDateTime.now()));

        applicationEventPublisher.publishEvent(new BuzzCommentedEvent(
                buzz.getId(), thread.getId(), buzz.getCreatorId(), thread.getStarterId(), command.authorId()));
        return comment.getId();
    }

    public void updateComment(UpdateBuzzCommentCommand command) {
        Buzz buzz = buzzOf(command.buzzId());
        BuzzThread thread = threadOf(buzz, command.threadId());
        BuzzComment comment = commentOf(thread, command.commentId());

        comment.update(command.requesterId(), command.text(), command.imageKeys());
        buzzCommentRepository.save(comment);
    }

    public void deleteComment(Long requesterId, Long buzzId, Long threadId, Long commentId) {
        Buzz buzz = buzzOf(buzzId);
        BuzzThread thread = threadOf(buzz, threadId);
        BuzzComment comment = commentOf(thread, commentId);
        comment.validateDeletion(requesterId, buzz);

        buzzCommentRepository.deleteById(commentId);
    }

    private boolean isAudienceMember(Buzz buzz, Long userId) {
        return !buzz.isCreator(userId)
                && buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), userId).isPresent();
    }

    private boolean isFriendOfStarter(Long userId, Buzz buzz, BuzzThread thread) {
        if (!thread.needsFriendshipToAccess(userId, buzz)) {
            return false;
        }
        return buzzSocialPort.getFriendIdsIn(userId, List.of(thread.getStarterId()))
                .contains(thread.getStarterId());
    }

    private Buzz buzzOf(Long buzzId) {
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
