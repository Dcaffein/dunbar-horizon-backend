package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzCommentResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadDetailResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler.Profiles;
import com.example.DunbarHorizon.buzz.application.service.BuzzThreadAccess.ThreadContext;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BuzzThreadService {

    private final BuzzRepository buzzRepository;
    private final BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    private final BuzzThreadRepository buzzThreadRepository;
    private final BuzzCommentRepository buzzCommentRepository;
    private final BuzzThreadAccess buzzThreadAccess;
    private final BuzzSocialPort buzzSocialPort;
    private final BuzzProfileAssembler buzzProfileAssembler;
    private final ImageStoragePort imageStoragePort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Long createThread(CreateBuzzThreadCommand command) {
        Buzz buzz = buzzRepository.findByIdForShare(command.buzzId())
                .orElseThrow(() -> new BuzzNotFoundException(command.buzzId()));
        Optional<BuzzAudienceMember> member =
                buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), command.starterId());

        BuzzThread thread = buzzThreadRepository.save(BuzzThread.create(
                buzz, command.starterId(), member.isPresent(), command.visibility(), command.text(),
                command.imageKeys(), LocalDateTime.now()));
        member.ifPresent(BuzzAudienceMember::markAsRead);

        applicationEventPublisher.publishEvent(new BuzzThreadCreatedEvent(
                buzz.getId(), thread.getId(), buzz.getCreatorId(), command.starterId()));
        return thread.getId();
    }

    @Transactional(readOnly = true)
    public BuzzThreadDetailResult getThread(Long viewerId, Long buzzId, Long threadId) {
        ThreadContext context = buzzThreadAccess.forReading(viewerId, buzzId, threadId, LocalDateTime.now());
        Buzz buzz = context.buzz();
        BuzzThread thread = context.thread();
        List<BuzzComment> comments = buzzCommentRepository.findAllByThreadId(threadId);

        Set<Long> viewerIds = viewerIdsOf(buzz, thread);
        Set<Long> profileIds = new LinkedHashSet<>(viewerIds);
        comments.forEach(comment -> profileIds.add(comment.getAuthorId()));
        Profiles profiles = buzzProfileAssembler.profilesOf(profileIds);

        return BuzzThreadDetailResult.of(
                thread,
                profiles.of(thread.getStarterId()),
                imageStoragePort.resolveUrls(thread.getImageKeys()),
                viewerId,
                profiles.foundOf(viewerIds),
                comments.stream()
                        .map(comment -> BuzzCommentResult.of(
                                comment,
                                profiles.of(comment.getAuthorId()),
                                imageStoragePort.resolveUrls(comment.getImageKeys()),
                                viewerId))
                        .toList());
    }

    public void updateThread(UpdateBuzzThreadCommand command) {
        Buzz buzz = buzzRepository.findById(command.buzzId())
                .orElseThrow(() -> new BuzzNotFoundException(command.buzzId()));
        BuzzThread thread = threadOf(buzz, command.threadId());

        thread.update(command.requesterId(), command.text(), command.imageKeys());
    }

    public void deleteThread(Long requesterId, Long buzzId, Long threadId) {
        Buzz buzz = buzzRepository.findByIdForShare(buzzId)
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

    private Set<Long> viewerIdsOf(Buzz buzz, BuzzThread thread) {
        Set<Long> viewerIds = new LinkedHashSet<>();
        viewerIds.add(buzz.getCreatorId());
        viewerIds.add(thread.getStarterId());
        if (thread.isPublic()) {
            List<Long> audienceIds = buzzAudienceMemberRepository.findUserIdsByBuzzId(buzz.getId());
            Set<Long> starterFriendIds = buzzSocialPort.getFriendIdsIn(thread.getStarterId(), audienceIds);
            audienceIds.stream().filter(starterFriendIds::contains).forEach(viewerIds::add);
        }
        return viewerIds;
    }
}
