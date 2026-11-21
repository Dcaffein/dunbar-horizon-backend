package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzProfileResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BuzzService {

    private final RecipientStrategyProvider strategyProvider;
    private final BuzzRepository buzzRepository;
    private final BuzzSocialPort buzzSocialPort;
    private final ImageStoragePort imageStoragePort;
    private final ApplicationEventPublisher eventPublisher;

    public void createBuzz(CreateBuzzCommand command, List<String> imageKeys) {
        BuzzCreatorInfo author = buzzSocialPort.getCreatorProfiles(List.of(command.creatorId()))
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("작성자 정보를 찾을 수 없습니다."));
        RecipientStrategyPort strategy = strategyProvider.getStrategy(command.spec().getType());
        Set<Long> recipientIds = strategy.fetchRecipientIds(command.creatorId(), command.spec());

        List<String> imageUrls = imageStoragePort.resolveUrls(imageKeys);

        Buzz buzz = Buzz.builder()
                .creatorId(command.creatorId())
                .creatorNickname(author.nickname())
                .creatorProfileImageUrl(author.profileImageUrl())
                .text(command.text())
                .imageUrls(imageUrls)
                .recipientIds(new ArrayList<>(recipientIds))
                .build();

        Buzz savedBuzz = buzzRepository.save(buzz);
        eventPublisher.publishEvent(new BuzzCreatedEvent(savedBuzz.getId(), command.creatorId(), recipientIds));
    }

    public void deleteBuzz(Long requesterId, String buzzId) {
        Buzz buzz = getBuzzOrThrow(buzzId);
        buzz.validateDeletion(requesterId);
        buzzRepository.deleteById(buzzId);
    }

    public Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable) {
        Set<Long> excludedIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzRepository.findAllByRecipientId(userId, excludedIds, pageable)
                .map(buzz -> BuzzSummaryResult.from(buzz, userId));
    }

    public Slice<BuzzSummaryResult> getSentBuzzes(Long userId, Pageable pageable) {
        return buzzRepository.findAllByCreatorId(userId, pageable)
                .map(buzz -> BuzzSummaryResult.from(buzz, userId));
    }

    public BuzzDetailResult getBuzzDetail(Long userId, String buzzId) {
        Buzz buzz = getBuzzOrThrow(buzzId);
        buzz.validateAccess(userId);
        buzzRepository.addReadRecipient(buzzId, userId);
        List<BuzzProfileResult> recipients = buzzSocialPort.getCreatorProfiles(buzz.getRecipientIds()).stream()
                .map(info -> new BuzzProfileResult(info.id(), info.nickname(), info.profileImageUrl()))
                .toList();
        return BuzzDetailResult.from(buzz, userId, recipients);
    }

    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzRepository.findUnreadSenderIds(userId, excludedIds);
    }

    private Buzz getBuzzOrThrow(String buzzId) {
        return requireBuzz(buzzRepository.findById(buzzId), buzzId);
    }

    private Buzz requireBuzz(Buzz buzz, String buzzId) {
        if (buzz == null) {
            throw new BuzzNotFoundException(buzzId);
        }
        return buzz;
    }
}
