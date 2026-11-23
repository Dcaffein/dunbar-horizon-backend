package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.view.BuzzAudienceView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzDetailView;
import com.example.DunbarHorizon.buzz.application.dto.view.BuzzThreadView;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
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
        BuzzDetailView buzz = buzzQueryRepository.findDetail(buzzId)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        List<BuzzAudienceView> audience = buzzQueryRepository.findAudience(buzzId);

        boolean isCreator = buzz.creatorId().equals(userId);
        Optional<BuzzAudienceView> member = isCreator
                ? Optional.empty()
                : audience.stream().filter(audienceMember -> audienceMember.userId().equals(userId)).findFirst();
        boolean isAudienceMember = member.isPresent();
        Buzz.validateAccess(buzzId, buzz.creatorId(), userId, isAudienceMember);

        return BuzzDetailResult.of(
                buzz,
                buzzProfileAssembler.profileOf(buzz.creatorId(), buzz.creatorProfile()),
                imageStoragePort.resolveUrls(buzz.imageKeys()),
                member.map(audienceMember -> !audienceMember.isRead()).orElse(false),
                isCreator,
                audience.stream()
                        .filter(audienceMember -> audienceMember.profile() != null)
                        .map(audienceMember -> buzzProfileAssembler.profileOf(audienceMember.userId(), audienceMember.profile()))
                        .toList(),
                accessibleThreads(buzz, userId).stream()
                        .map(thread -> BuzzThreadSummaryResult.of(
                                thread, buzzProfileAssembler.profileOf(thread.starterId(), thread.starterProfile())))
                        .toList());
    }

    public Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable) {
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzQueryRepository.findReceived(userId, excludedCreatorIds, pageable)
                .map(buzz -> BuzzSummaryResult.of(
                        buzz,
                        buzzProfileAssembler.profileOf(buzz.creatorId(), buzz.creatorProfile()),
                        imageStoragePort.resolveUrls(buzz.imageKeys()),
                        !buzz.isRead(),
                        false));
    }

    public Slice<BuzzSummaryResult> getSentBuzzes(Long userId, Pageable pageable) {
        return buzzQueryRepository.findSent(userId, pageable)
                .map(buzz -> BuzzSummaryResult.of(
                        buzz,
                        buzzProfileAssembler.profileOf(buzz.creatorId(), buzz.creatorProfile()),
                        imageStoragePort.resolveUrls(buzz.imageKeys()),
                        false,
                        true));
    }

    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzQueryRepository.findUnreadCreatorIds(userId, excludedCreatorIds);
    }

    private List<BuzzThreadView> accessibleThreads(BuzzDetailView buzz, Long userId) {
        Set<Long> startersToCheck = buzz.threads().stream()
                .filter(thread -> BuzzThread.needsFriendshipToAccess(
                        thread.visibility(), thread.starterId(), buzz.creatorId(), userId))
                .map(BuzzThreadView::starterId)
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
