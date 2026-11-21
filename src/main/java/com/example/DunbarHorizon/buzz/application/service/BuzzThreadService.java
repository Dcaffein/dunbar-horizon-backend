package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.OpenBuzzThreadCommand;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadDetailResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler.Profiles;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
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
    private final BuzzSocialPort buzzSocialPort;
    private final BuzzProfileAssembler buzzProfileAssembler;
    private final ImageStoragePort imageStoragePort;

    public Long openThread(OpenBuzzThreadCommand command) {
        LocalDateTime now = LocalDateTime.now();
        Buzz buzz = buzzRepository.findByIdForShare(command.buzzId())
                .orElseThrow(() -> new BuzzNotFoundException(command.buzzId()));
        Optional<BuzzAudienceMember> member =
                buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzz.getId(), command.starterId());
        buzz.validateThreadOpening(command.starterId(), member.isPresent(), now);

        BuzzThread thread = buzzThreadRepository.save(BuzzThread.open(
                buzz.getId(), command.starterId(), command.visibility(), command.text(), command.imageKeys(), now));
        member.ifPresent(BuzzAudienceMember::markAsRead);
        return thread.getId();
    }

    @Transactional(readOnly = true)
    public BuzzThreadDetailResult getThread(Long viewerId, String buzzId, Long threadId) {
        LocalDateTime now = LocalDateTime.now();
        Buzz buzz = buzzRepository.findById(buzzId)
                .filter(found -> !found.isExpired(now))
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        boolean isAudienceMember = !buzz.isCreator(viewerId)
                && buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzzId, viewerId).isPresent();
        buzz.validateAccess(viewerId, isAudienceMember);

        BuzzThread thread = buzzThreadRepository.findById(threadId)
                .filter(found -> found.getBuzzId().equals(buzzId))
                .orElseThrow(() -> new BuzzThreadNotFoundException(buzzId, threadId));
        thread.validateAccess(viewerId, buzz.getCreatorId(), isFriendOfStarter(viewerId, buzz, thread));

        Set<Long> viewerIds = viewerIdsOf(buzz, thread);
        Profiles profiles = buzzProfileAssembler.profilesOf(viewerIds);
        return BuzzThreadDetailResult.of(
                thread,
                profiles.of(thread.getStarterId()),
                imageStoragePort.resolveUrls(thread.getImageKeys()),
                viewerId,
                profiles.foundOf(viewerIds));
    }

    private boolean isFriendOfStarter(Long viewerId, Buzz buzz, BuzzThread thread) {
        if (!thread.needsFriendshipToAccess(viewerId, buzz.getCreatorId())) {
            return false;
        }
        return buzzSocialPort.getFriendIdsIn(viewerId, List.of(thread.getStarterId()))
                .contains(thread.getStarterId());
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
