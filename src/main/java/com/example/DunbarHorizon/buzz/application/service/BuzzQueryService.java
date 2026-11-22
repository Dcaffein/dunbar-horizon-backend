package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzAudienceInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler.Profiles;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
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

    private final BuzzQueryRepository buzzQueryRepository;
    private final BuzzSocialPort buzzSocialPort;
    private final BuzzProfileAssembler buzzProfileAssembler;
    private final ImageStoragePort imageStoragePort;

    public BuzzDetailResult getBuzzDetail(Long userId, Long buzzId) {
        LocalDateTime now = LocalDateTime.now();
        BuzzDetailInfo buzz = buzzQueryRepository.findDetail(buzzId, now)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        List<BuzzAudienceInfo> audience = buzzQueryRepository.findAudience(buzzId);

        boolean isCreator = buzz.creatorId().equals(userId);
        Optional<BuzzAudienceInfo> member = isCreator
                ? Optional.empty()
                : audience.stream().filter(audienceMember -> audienceMember.userId().equals(userId)).findFirst();
        Buzz.validateAccess(buzzId, buzz.creatorId(), userId, member.isPresent());

        List<Long> audienceIds = audience.stream().map(BuzzAudienceInfo::userId).toList();
        List<Long> profileIds = new ArrayList<>(audienceIds);
        profileIds.add(buzz.creatorId());
        Profiles profiles = buzzProfileAssembler.profilesOf(profileIds);

        return BuzzDetailResult.of(
                buzz,
                profiles.of(buzz.creatorId()),
                imageStoragePort.resolveUrls(buzz.imageKeys()),
                member.map(audienceMember -> !audienceMember.isRead()).orElse(false),
                isCreator,
                profiles.foundOf(audienceIds),
                accessibleThreads(buzz, userId).stream()
                        .map(thread -> BuzzThreadSummaryResult.of(thread, profiles.of(thread.starterId())))
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

    private List<BuzzThreadInfo> accessibleThreads(BuzzDetailInfo buzz, Long userId) {
        Set<Long> startersToCheck = buzz.threads().stream()
                .filter(thread -> BuzzThread.needsFriendshipToAccess(
                        thread.visibility(), thread.starterId(), buzz.creatorId(), userId))
                .map(BuzzThreadInfo::starterId)
                .collect(Collectors.toSet());
        Set<Long> friendStarterIds = startersToCheck.isEmpty()
                ? Set.of()
                : buzzSocialPort.getFriendIdsIn(userId, startersToCheck);

        return buzz.threads().stream()
                .filter(thread -> BuzzThread.isAccessibleBy(thread.visibility(), thread.starterId(), buzz.creatorId(),
                        userId, friendStarterIds.contains(thread.starterId())))
                .toList();
    }
}
