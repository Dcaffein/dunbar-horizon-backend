package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.event.BuzzThreadCreatedEvent;
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

@Service
@RequiredArgsConstructor
@Transactional
public class BuzzThreadService {

    private final BuzzRepository buzzRepository;
    private final BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    private final BuzzThreadRepository buzzThreadRepository;
    private final BuzzCommentRepository buzzCommentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Long createThread(CreateBuzzThreadCommand command) {
        Buzz buzz = buzzRepository.findByIdForShare(command.buzzId())
                .orElseThrow(() -> new BuzzNotFoundException(command.buzzId()));
        boolean isAudienceMember =
                buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), command.starterId()).isPresent();

        BuzzThread thread = buzzThreadRepository.save(BuzzThread.create(
                buzz, command.starterId(), isAudienceMember, command.visibility(), command.text(),
                command.imageKeys(), LocalDateTime.now()));

        applicationEventPublisher.publishEvent(new BuzzThreadCreatedEvent(
                buzz.getId(), thread.getId(), buzz.getCreatorId(), command.starterId()));
        return thread.getId();
    }

    public void updateThread(UpdateBuzzThreadCommand command) {
        Buzz buzz = buzzRepository.findById(command.buzzId())
                .orElseThrow(() -> new BuzzNotFoundException(command.buzzId()));
        BuzzThread thread = threadOf(buzz, command.threadId());

        thread.update(command.requesterId(), command.text(), command.imageKeys());
        buzzThreadRepository.save(thread);
    }

    public void deleteThread(Long requesterId, Long buzzId, Long threadId) {
        Buzz buzz = buzzRepository.findById(buzzId)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        BuzzThread thread = threadOf(buzz, threadId);
        thread.validateDeletion(requesterId, buzz);

        buzzThreadRepository.deleteById(threadId);
        buzzCommentRepository.deleteAllByThreadId(threadId);
    }

    private BuzzThread threadOf(Buzz buzz, Long threadId) {
        return buzzThreadRepository.findById(threadId)
                .filter(thread -> thread.getBuzzId().equals(buzz.getId()))
                .orElseThrow(() -> new BuzzThreadNotFoundException(buzz.getId(), threadId));
    }
}
