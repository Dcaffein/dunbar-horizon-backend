package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler.Profiles;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuzzQueryService {

    private final BuzzRepository buzzRepository;
    private final BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    private final BuzzThreadRepository buzzThreadRepository;
    private final BuzzQueryRepository buzzQueryRepository;
    private final BuzzSocialPort buzzSocialPort;
    private final BuzzProfileAssembler buzzProfileAssembler;
    private final ImageStoragePort imageStoragePort;

    public BuzzDetailResult getBuzzDetail(Long userId, Long buzzId) {
        LocalDateTime now = LocalDateTime.now();
        Buzz buzz = buzzRepository.findById(buzzId)
                .filter(found -> !found.isExpired(now))
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));

        Optional<BuzzAudienceMember> member = buzz.isCreator(userId)
                ? Optional.empty()
                : buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzzId, userId);
        buzz.validateAccess(userId, member.isPresent());

        List<Long> audienceIds = buzzAudienceMemberRepository.findUserIdsByBuzzId(buzzId);
        List<BuzzThread> threads = accessibleThreads(buzz, userId);

        List<Long> profileIds = new ArrayList<>(audienceIds);
        profileIds.add(buzz.getCreatorId());
        Profiles profiles = buzzProfileAssembler.profilesOf(profileIds);

        return BuzzDetailResult.of(
                buzz,
                profiles.of(buzz.getCreatorId()),
                imageStoragePort.resolveUrls(buzz.getImageKeys()),
                member.map(BuzzAudienceMember::isUnread).orElse(false),
                buzz.isCreator(userId),
                profiles.foundOf(audienceIds),
                threads.stream()
                        .map(thread -> BuzzThreadSummaryResult.of(thread, profiles.of(thread.getStarterId())))
                        .toList(),
                now);
    }

    public Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        Slice<ReceivedBuzz> receivedBuzzes =
                buzzQueryRepository.findReceived(userId, excludedCreatorIds, now, pageable);

        Profiles profiles = buzzProfileAssembler.profilesOf(receivedBuzzes.getContent().stream()
                .map(received -> received.buzz().getCreatorId())
                .toList());
        return receivedBuzzes.map(received -> BuzzSummaryResult.of(
                received.buzz(),
                profiles.of(received.buzz().getCreatorId()),
                imageStoragePort.resolveUrls(received.buzz().getImageKeys()),
                !received.read(),
                false,
                now));
    }

    public Slice<BuzzSummaryResult> getSentBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Slice<Buzz> sentBuzzes = buzzQueryRepository.findSent(userId, now, pageable);

        Profiles profiles = buzzProfileAssembler.profilesOf(List.of(userId));
        return sentBuzzes.map(buzz -> BuzzSummaryResult.of(
                buzz,
                profiles.of(userId),
                imageStoragePort.resolveUrls(buzz.getImageKeys()),
                false,
                true,
                now));
    }

    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzQueryRepository.findUnreadCreatorIds(userId, excludedCreatorIds, LocalDateTime.now());
    }

    private List<BuzzThread> accessibleThreads(Buzz buzz, Long userId) {
        List<BuzzThread> threads = buzzThreadRepository.findAllByBuzzId(buzz.getId());
        Set<Long> startersToCheck = threads.stream()
                .filter(thread -> thread.needsFriendshipToAccess(userId, buzz))
                .map(BuzzThread::getStarterId)
                .collect(Collectors.toSet());
        Set<Long> friendStarterIds = startersToCheck.isEmpty()
                ? Set.of()
                : buzzSocialPort.getFriendIdsIn(userId, startersToCheck);

        return threads.stream()
                .filter(thread -> thread.isAccessibleBy(
                        userId, buzz, friendStarterIds.contains(thread.getStarterId())))
                .toList();
    }
}
