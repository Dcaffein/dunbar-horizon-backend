package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.command.UpdateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.application.service.BuzzProfileAssembler.Profiles;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzCommentRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
@Transactional
public class BuzzService {

    private final RecipientStrategyProvider recipientStrategyProvider;
    private final BuzzRepository buzzRepository;
    private final BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    private final BuzzThreadRepository buzzThreadRepository;
    private final BuzzCommentRepository buzzCommentRepository;
    private final BuzzQueryRepository buzzQueryRepository;
    private final BuzzSocialPort buzzSocialPort;
    private final BuzzProfileAssembler buzzProfileAssembler;
    private final ImageStoragePort imageStoragePort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void createBuzz(CreateBuzzCommand command, List<String> imageKeys) {
        RecipientStrategyPort strategy = recipientStrategyProvider.getStrategy(command.spec().getType());
        Set<Long> audienceIds = strategy.fetchRecipientIds(command.creatorId(), command.spec());

        Buzz buzz = Buzz.create(command.creatorId(), command.text(), imageKeys, audienceIds, LocalDateTime.now());
        buzzRepository.save(buzz);
        buzzAudienceMemberRepository.saveAll(audienceIds.stream()
                .map(userId -> BuzzAudienceMember.create(buzz.getId(), userId))
                .toList());

        applicationEventPublisher.publishEvent(new BuzzCreatedEvent(buzz.getId(), command.creatorId(), audienceIds));
    }

    public void updateBuzz(UpdateBuzzCommand command) {
        Buzz buzz = buzzRepository.findById(command.buzzId())
                .orElseThrow(() -> new BuzzNotFoundException(command.buzzId()));
        buzz.update(command.requesterId(), command.text(), command.imageKeys(), LocalDateTime.now());
    }

    public void deleteBuzz(Long requesterId, String buzzId) {
        Buzz buzz = buzzRepository.findById(buzzId)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        buzz.validateDeletion(requesterId);

        buzzRepository.deleteById(buzzId);
        buzzAudienceMemberRepository.deleteAllByBuzzId(buzzId);
        buzzCommentRepository.deleteAllByBuzzId(buzzId);
        buzzThreadRepository.deleteAllByBuzzId(buzzId);
    }

    public int purgeExpiredBuzzes(LocalDateTime now) {
        List<String> expiredBuzzIds = buzzRepository.findExpiredIds(now);
        if (expiredBuzzIds.isEmpty()) {
            return 0;
        }
        buzzRepository.deleteAllByIds(expiredBuzzIds);
        buzzAudienceMemberRepository.deleteAllByBuzzIds(expiredBuzzIds);
        buzzCommentRepository.deleteAllByBuzzIds(expiredBuzzIds);
        buzzThreadRepository.deleteAllByBuzzIds(expiredBuzzIds);
        return expiredBuzzIds.size();
    }

    @Transactional(readOnly = true)
    public Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        Slice<ReceivedBuzz> receivedBuzzes =
                buzzQueryRepository.findUnexpiredReceived(userId, excludedCreatorIds, now, pageable);

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

    @Transactional(readOnly = true)
    public Slice<BuzzSummaryResult> getSentBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Slice<Buzz> sentBuzzes = buzzQueryRepository.findUnexpiredSent(userId, now, pageable);

        Profiles profiles = buzzProfileAssembler.profilesOf(List.of(userId));
        return sentBuzzes.map(buzz -> BuzzSummaryResult.of(
                buzz,
                profiles.of(userId),
                imageStoragePort.resolveUrls(buzz.getImageKeys()),
                false,
                true,
                now));
    }

    public BuzzDetailResult getBuzzDetail(Long userId, String buzzId) {
        LocalDateTime now = LocalDateTime.now();
        Buzz buzz = buzzRepository.findByIdForShare(buzzId)
                .filter(found -> !found.isExpired(now))
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));

        Optional<BuzzAudienceMember> member = buzz.isCreator(userId)
                ? Optional.empty()
                : buzzAudienceMemberRepository.findByBuzzIdAndUserId(buzzId, userId);
        buzz.validateAccess(userId, member.isPresent());

        boolean isUnread = member.map(BuzzAudienceMember::isUnread).orElse(false);
        member.ifPresent(BuzzAudienceMember::markAsRead);

        List<Long> audienceIds = buzzAudienceMemberRepository.findUserIdsByBuzzId(buzzId);
        List<BuzzThread> threads = accessibleThreads(buzz, userId);

        List<Long> profileIds = new ArrayList<>(audienceIds);
        profileIds.add(buzz.getCreatorId());
        Profiles profiles = buzzProfileAssembler.profilesOf(profileIds);

        return BuzzDetailResult.of(
                buzz,
                profiles.of(buzz.getCreatorId()),
                imageStoragePort.resolveUrls(buzz.getImageKeys()),
                isUnread,
                buzz.isCreator(userId),
                profiles.foundOf(audienceIds),
                threads.stream()
                        .map(thread -> BuzzThreadSummaryResult.of(thread, profiles.of(thread.getStarterId())))
                        .toList(),
                now);
    }

    @Transactional(readOnly = true)
    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzQueryRepository.findUnexpiredUnreadCreatorIds(userId, excludedCreatorIds, LocalDateTime.now());
    }

    private List<BuzzThread> accessibleThreads(Buzz buzz, Long userId) {
        List<BuzzThread> threads = buzzThreadRepository.findAllByBuzzId(buzz.getId());
        Set<Long> startersToCheck = threads.stream()
                .filter(thread -> thread.needsFriendshipToAccess(userId, buzz.getCreatorId()))
                .map(BuzzThread::getStarterId)
                .collect(Collectors.toSet());
        Set<Long> friendStarterIds = startersToCheck.isEmpty()
                ? Set.of()
                : buzzSocialPort.getFriendIdsIn(userId, startersToCheck);

        return threads.stream()
                .filter(thread -> thread.isAccessibleBy(
                        userId, buzz.getCreatorId(), friendStarterIds.contains(thread.getStarterId())))
                .toList();
    }
}
