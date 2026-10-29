package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.port.in.BuzzCommandUseCase;
import com.example.DunbarHorizon.buzz.application.port.in.BuzzQueryUseCase;
import com.example.DunbarHorizon.buzz.application.port.in.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzProfileResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCommentedEvent;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzCommentNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
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
public class BuzzService implements BuzzCommandUseCase, BuzzQueryUseCase {

    private final RecipientStrategyProvider strategyProvider;
    private final BuzzRepository buzzRepository;
    private final BuzzSocialPort buzzSocialPort;
    private final ImageStoragePort imageStoragePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
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

    @Override
    public void commentOnBuzz(Long commenterId, String buzzId, String commentText, List<String> imageKeys, boolean isPublic) {
        BuzzCreatorInfo commenterProfile = buzzSocialPort.getCreatorProfiles(List.of(commenterId))
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("사용자 정보를 찾을 수 없습니다."));

        Buzz buzz = getBuzzOrThrow(buzzId);
        buzz.validateCommentCreation(commenterId);
        List<String> imageUrls = imageStoragePort.resolveUrls(imageKeys);

        BuzzComment comment = BuzzComment.create(
                commenterId,
                commenterProfile.nickname(),
                commenterProfile.profileImageUrl(),
                commentText,
                imageUrls,
                isPublic
        );
        buzzRepository.addComment(buzzId, comment, buzz.isRecipient(commenterId));
        eventPublisher.publishEvent(new BuzzCommentedEvent(buzzId, buzz.getCreatorId(), commenterId));
    }

    @Override
    public void updateComment(Long requesterId, String buzzId, String commentId, String commentText, List<String> imageKeys) {
        Buzz buzz = getBuzzOrThrow(buzzId);
        buzz.validateNotExpired();
        BuzzComment comment = getCommentOrThrow(buzzId, commentId);

        List<String> imageUrls = imageStoragePort.resolveUrls(imageKeys);
        comment.update(requesterId, commentText, imageUrls);
        buzzRepository.updateComment(buzzId, commentId, comment.getText(), comment.getImageUrls());
    }

    @Override
    public void deleteComment(Long requesterId, String buzzId, String commentId) {
        Buzz buzz = getBuzzOrThrow(buzzId);
        buzz.validateNotExpired();
        BuzzComment comment = getCommentOrThrow(buzzId, commentId);

        comment.validateDeletionAuthority(requesterId, buzz.getCreatorId());
        buzzRepository.removeComment(buzzId, commentId);
    }

    @Override
    public void deleteBuzz(Long requesterId, String buzzId) {
        Buzz buzz = getBuzzOrThrow(buzzId);
        buzz.validateDeletion(requesterId);
        buzzRepository.deleteById(buzzId);
    }

    @Override
    public Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable) {
        Set<Long> excludedIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzRepository.findAllByRecipientId(userId, excludedIds, pageable)
                .map(buzz -> BuzzSummaryResult.from(buzz, userId));
    }

    @Override
    public Slice<BuzzSummaryResult> getSentBuzzes(Long userId, Pageable pageable) {
        return buzzRepository.findAllByCreatorId(userId, pageable)
                .map(buzz -> BuzzSummaryResult.from(buzz, userId));
    }

    @Override
    public BuzzDetailResult getBuzzDetail(Long userId, String buzzId) {
        Buzz buzz = getBuzzWithCommentsOrThrow(buzzId);
        buzz.validateAccess(userId);
        buzzRepository.addReadRecipient(buzzId, userId);
        List<BuzzProfileResult> recipients = buzzSocialPort.getCreatorProfiles(buzz.getRecipientIds()).stream()
                .map(info -> new BuzzProfileResult(info.id(), info.nickname(), info.profileImageUrl()))
                .toList();
        return BuzzDetailResult.from(buzz, userId, recipients);
    }

    @Override
    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzRepository.findUnreadSenderIds(userId, excludedIds);
    }

    private Buzz getBuzzOrThrow(String buzzId) {
        return requireBuzz(buzzRepository.findById(buzzId), buzzId);
    }

    private Buzz getBuzzWithCommentsOrThrow(String buzzId) {
        return requireBuzz(buzzRepository.findByIdWithRecentComments(buzzId), buzzId);
    }

    private Buzz requireBuzz(Buzz buzz, String buzzId) {
        if (buzz == null) {
            throw new BuzzNotFoundException(buzzId);
        }
        return buzz;
    }

    private BuzzComment getCommentOrThrow(String buzzId, String commentId) {
        return buzzRepository.findComment(buzzId, commentId)
                .orElseThrow(() -> new BuzzCommentNotFoundException(buzzId, commentId));
    }
}
