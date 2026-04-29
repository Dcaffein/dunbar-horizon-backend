package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.port.in.BuzzCommandUseCase;
import com.example.DunbarHorizon.buzz.application.port.in.BuzzQueryUseCase;
import com.example.DunbarHorizon.buzz.application.port.in.command.CreateBuzzCommand;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzCreatorInfo;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzDetailResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzSummaryResult;
import com.example.DunbarHorizon.buzz.domain.event.BuzzReadEvent;
import com.example.DunbarHorizon.buzz.domain.repository.BuzzRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCommentedEvent;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public void createBuzz(CreateBuzzCommand command, List<MultipartFile> images) {
        BuzzCreatorInfo author = buzzSocialPort.getCreatorProfiles(List.of(command.creatorId()))
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("작성자 정보를 찾을 수 없습니다."));
        RecipientStrategyPort strategy = strategyProvider.getStrategy(command.spec().getType());
        Set<Long> recipientIds = strategy.fetchRecipientIds(command.creatorId(), command.spec());

        List<String> imageUrls = imageStoragePort.upload(images);

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
    public void commentOnBuzz(Long commenterId, String buzzId, String commentText, List<MultipartFile> images, boolean isPublic) {
        BuzzCreatorInfo commenterProfile = buzzSocialPort.getCreatorProfiles(List.of(commenterId))
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("사용자 정보를 찾을 수 없습니다."));

        Buzz buzz = getBuzzOrThrow(buzzId);
        List<String> imageUrls = imageStoragePort.upload(images);

        BuzzComment comment = buzz.createComment(
                commenterId,
                commenterProfile.nickname(),
                commenterProfile.profileImageUrl(),
                commentText,
                imageUrls,
                isPublic
        );
        buzzRepository.addComment(buzzId, comment);
        eventPublisher.publishEvent(new BuzzCommentedEvent(buzzId, buzz.getCreatorId(), commenterId));
    }

    @Override
    public void updateComment(Long requesterId, String buzzId, String commentId, String commentText, List<MultipartFile> images) {
        Buzz buzz = getBuzzOrThrow(buzzId);
        List<String> imageUrls = imageStoragePort.upload(images);
        buzz.updateComment(requesterId, commentId, commentText, imageUrls);
        buzzRepository.updateComment(buzzId, commentId, commentText, imageUrls);
    }

    @Override
    public void deleteComment(Long requesterId, String buzzId, String commentId) {
        Buzz buzz = getBuzzOrThrow(buzzId);
        buzz.validateCommentDeletion(requesterId, commentId);
        buzzRepository.removeComment(buzzId, commentId);
    }

    @Override
    public void deleteBuzz(Long requesterId, String buzzId) {
        Buzz buzz = getBuzzOrThrow(buzzId);
        if (!buzz.getCreatorId().equals(requesterId)) {
            throw new BuzzAccessDeniedException("버즈 삭제 권한이 없습니다.");
        }
        buzzRepository.deleteById(buzzId);
    }

    @Override
    public Slice<BuzzSummaryResult> getReceivedBuzzes(Long userId, Pageable pageable) {
        Set<Long> excludedIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzRepository.findAllByRecipientId(userId, excludedIds, pageable)
                .map(buzz -> BuzzSummaryResult.from(buzz, userId));
    }

    @Override
    public BuzzDetailResult getBuzzDetail(Long userId, String buzzId) {
        Buzz buzz = getBuzzOrThrow(buzzId);

        if (!buzz.isRecipient(userId) && !buzz.getCreatorId().equals(userId)) {
            throw new BuzzAccessDeniedException("접근 권한이 없습니다.");
        }

        eventPublisher.publishEvent(new BuzzReadEvent(buzzId, userId));
        return BuzzDetailResult.from(buzz, userId);
    }

    @Override
    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedIds = buzzSocialPort.getNonReceivableFriendIds(userId);
        return buzzRepository.findUnreadSenderIds(userId, excludedIds);
    }

    private Buzz getBuzzOrThrow(String buzzId) {
        Buzz buzz = buzzRepository.findById(buzzId);
        if (buzz == null) {
            throw new BuzzNotFoundException();
        }
        return buzz;
    }
}
