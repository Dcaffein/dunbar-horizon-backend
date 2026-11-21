package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.ReceivedBuzz;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzProfileResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzAudienceMemberRepository;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BuzzService {

    private final RecipientStrategyProvider recipientStrategyProvider;
    private final BuzzRepository buzzRepository;
    private final BuzzAudienceMemberRepository buzzAudienceMemberRepository;
    private final BuzzQueryRepository buzzQueryRepository;
    private final BuzzSocialPort buzzSocialPort;
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

    public void deleteBuzz(Long requesterId, String buzzId) {
        Buzz buzz = buzzRepository.findById(buzzId)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        buzz.validateDeletion(requesterId);

        buzzRepository.deleteById(buzzId);
        buzzAudienceMemberRepository.deleteAllByBuzzId(buzzId);
    }

    @Transactional(readOnly = true)
    public Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        Slice<ReceivedBuzz> receivedBuzzes =
                buzzQueryRepository.findUnexpiredReceived(userId, excludedCreatorIds, now, pageable);

        Map<Long, BuzzProfileResult> profiles = profilesOf(receivedBuzzes.getContent().stream()
                .map(received -> received.buzz().getCreatorId())
                .toList());
        return receivedBuzzes.map(received -> BuzzSummaryResult.of(
                received.buzz(),
                profileOf(profiles, received.buzz().getCreatorId()),
                imageStoragePort.resolveUrls(received.buzz().getImageKeys()),
                !received.read(),
                false,
                now));
    }

    @Transactional(readOnly = true)
    public Slice<BuzzSummaryResult> getSentBuzzes(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Slice<Buzz> sentBuzzes = buzzQueryRepository.findUnexpiredSent(userId, now, pageable);

        Map<Long, BuzzProfileResult> profiles = profilesOf(List.of(userId));
        return sentBuzzes.map(buzz -> BuzzSummaryResult.of(
                buzz,
                profileOf(profiles, userId),
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
        List<Long> profileIds = new ArrayList<>(audienceIds);
        profileIds.add(buzz.getCreatorId());
        Map<Long, BuzzProfileResult> profiles = profilesOf(profileIds);
        List<BuzzProfileResult> recipients = audienceIds.stream()
                .map(profiles::get)
                .filter(Objects::nonNull)
                .toList();

        return BuzzDetailResult.of(
                buzz,
                profileOf(profiles, buzz.getCreatorId()),
                imageStoragePort.resolveUrls(buzz.getImageKeys()),
                isUnread,
                buzz.isCreator(userId),
                recipients,
                now);
    }

    @Transactional(readOnly = true)
    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedCreatorIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzQueryRepository.findUnexpiredUnreadCreatorIds(userId, excludedCreatorIds, LocalDateTime.now());
    }

    private Map<Long, BuzzProfileResult> profilesOf(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return buzzSocialPort.getCreatorProfiles(new ArrayList<>(new LinkedHashSet<>(userIds))).stream()
                .map(BuzzService::toProfileResult)
                .collect(Collectors.toMap(BuzzProfileResult::userId, Function.identity(), (first, second) -> first));
    }

    private static BuzzProfileResult profileOf(Map<Long, BuzzProfileResult> profiles, Long userId) {
        return profiles.getOrDefault(userId, new BuzzProfileResult(userId, null, null));
    }

    private static BuzzProfileResult toProfileResult(BuzzCreatorInfo info) {
        return new BuzzProfileResult(info.id(), info.nickname(), info.profileImageUrl());
    }
}
