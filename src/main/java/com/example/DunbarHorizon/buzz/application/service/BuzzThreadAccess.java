package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BuzzThreadAccess {

    private final BuzzRepository buzzRepository;
    private final BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    private final BuzzThreadRepository buzzThreadRepository;
    private final BuzzSocialPort buzzSocialPort;

    public ThreadContext forReading(Long viewerId, String buzzId, Long threadId, LocalDateTime now) {
        Buzz buzz = buzzRepository.findById(buzzId)
                .filter(found -> !found.isExpired(now))
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        Optional<BuzzAudienceMember> member = memberOf(buzz, viewerId);
        buzz.validateAccess(viewerId, member.isPresent());

        BuzzThread thread = threadOf(buzzThreadRepository.findById(threadId), buzzId, threadId);
        thread.validateAccess(viewerId, buzz.getCreatorId(), isFriendOfStarter(viewerId, buzz, thread));
        return new ThreadContext(buzz, thread, member);
    }

    public ThreadContext forWriting(Long writerId, String buzzId, Long threadId, LocalDateTime now) {
        Buzz buzz = buzzRepository.findByIdForShare(buzzId)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        buzz.validateNotExpired(now);
        Optional<BuzzAudienceMember> member = memberOf(buzz, writerId);
        buzz.validateAccess(writerId, member.isPresent());

        BuzzThread thread = threadOf(buzzThreadRepository.findByIdForShare(threadId), buzzId, threadId);
        thread.validateAccess(writerId, buzz.getCreatorId(), isFriendOfStarter(writerId, buzz, thread));
        return new ThreadContext(buzz, thread, member);
    }

    private Optional<BuzzAudienceMember> memberOf(Buzz buzz, Long userId) {
        return buzz.isCreator(userId)
                ? Optional.empty()
                : buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), userId);
    }

    private static BuzzThread threadOf(Optional<BuzzThread> found, String buzzId, Long threadId) {
        return found.filter(thread -> thread.getBuzzId().equals(buzzId))
                .orElseThrow(() -> new BuzzThreadNotFoundException(buzzId, threadId));
    }

    private boolean isFriendOfStarter(Long userId, Buzz buzz, BuzzThread thread) {
        if (!thread.needsFriendshipToAccess(userId, buzz.getCreatorId())) {
            return false;
        }
        return buzzSocialPort.getFriendIdsIn(userId, List.of(thread.getStarterId()))
                .contains(thread.getStarterId());
    }

    public record ThreadContext(Buzz buzz, BuzzThread thread, Optional<BuzzAudienceMember> member) {
    }
}
