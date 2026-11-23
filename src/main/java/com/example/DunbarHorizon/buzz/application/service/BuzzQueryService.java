package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzAudienceInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzUserProfileInfo;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzProfileResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        return BuzzDetailResult.of(
                buzz,
                profileOf(buzz.creatorId(), buzz.creatorProfile()),
                imageStoragePort.resolveUrls(buzz.imageKeys()),
                member.map(audienceMember -> !audienceMember.isRead()).orElse(false),
                isCreator,
                audience.stream()
                        .filter(audienceMember -> audienceMember.profile() != null)
                        .map(audienceMember -> profileOf(audienceMember.userId(), audienceMember.profile()))
                        .toList(),
                accessibleThreads(buzz, userId).stream()
                        .map(thread -> BuzzThreadSummaryResult.of(
                                thread, profileOf(thread.starterId(), thread.starterProfile())))
                        .toList(),
                now);
    }

    public Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzQueryRepository.findReceived(userId, excludedCreatorIds, now, pageable)
                .map(buzz -> BuzzSummaryResult.of(
                        buzz,
                        profileOf(buzz.creatorId(), buzz.creatorProfile()),
                        imageStoragePort.resolveUrls(buzz.imageKeys()),
                        !buzz.isRead(),
                        false,
                        now));
    }

    public Slice<BuzzSummaryResult> getSentBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return buzzQueryRepository.findSent(userId, now, pageable)
                .map(buzz -> BuzzSummaryResult.of(
                        buzz,
                        profileOf(buzz.creatorId(), buzz.creatorProfile()),
                        imageStoragePort.resolveUrls(buzz.imageKeys()),
                        false,
                        true,
                        now));
    }

    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzQueryRepository.findUnreadCreatorIds(userId, excludedCreatorIds, LocalDateTime.now());
    }

    private BuzzProfileResult profileOf(Long userId, BuzzUserProfileInfo profile) {
        if (profile == null) {
            return new BuzzProfileResult(userId, null, null);
        }
        return new BuzzProfileResult(userId, profile.nickname(), imageStoragePort.resolveUrl(profile.profileImageKey()));
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
