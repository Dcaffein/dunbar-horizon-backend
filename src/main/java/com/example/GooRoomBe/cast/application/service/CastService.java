package com.example.GooRoomBe.cast.application.service;

import com.example.GooRoomBe.cast.application.service.recipient.RecipientStrategy;
import com.example.GooRoomBe.cast.application.service.recipient.RecipientStrategyProvider;
import com.example.GooRoomBe.cast.application.command.CreateCastCommand;
import com.example.GooRoomBe.cast.application.port.out.CastSocialPort;
import com.example.GooRoomBe.cast.application.port.out.dto.CastCreatorDto;
import com.example.GooRoomBe.cast.domain.event.CastCreatedEvent;
import com.example.GooRoomBe.cast.domain.event.CastRepliedEvent;
import com.example.GooRoomBe.cast.domain.exception.CastAccessDeniedException;
import com.example.GooRoomBe.cast.domain.exception.CastNotFoundException;
import com.example.GooRoomBe.cast.domain.model.Cast;
import com.example.GooRoomBe.cast.domain.model.CastReply;
import com.example.GooRoomBe.cast.domain.repository.CastRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CastService {
    private final RecipientStrategyProvider strategyProvider;
    private final CastRepository castRepository;
    private final CastSocialPort castSocialPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void createCast(CreateCastCommand command) {
        CastCreatorDto author = castSocialPort.getCreatorProfiles(List.of(command.creatorId()))
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("작성자 정보를 찾을 수 없습니다."));
        RecipientStrategy strategy = strategyProvider.getStrategy(command.payload().getType());
        Set<Long> recipientIds = strategy.fetchRecipientIds(command.creatorId(), command.payload());

        Cast cast = Cast.builder()
                .creatorId(command.creatorId())
                .creatorNickname(author.nickname())
                .creatorProfileImageUrl(author.profileImageUrl())
                .text(command.text())
                .imageUrls(command.imageUrls())
                .recipientIds(new ArrayList<>(recipientIds))
                .build();

        Cast savedCast = castRepository.save(cast);
        eventPublisher.publishEvent(new CastCreatedEvent(savedCast.getId(),command.creatorId(),recipientIds));
    }

    @Transactional
    public void replyToCast(Long replierId, String castId, String replyText,List<String> imageUrls, boolean isPublic) {
        CastCreatorDto replierProfile = castSocialPort.getCreatorProfiles(List.of(replierId))
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("사용자 정보를 찾을 수 없습니다."));

        Cast cast = getCastOrThrow(castId);

        CastReply reply = cast.createReply(
                replierId,
                replierProfile.nickname(),
                replierProfile.profileImageUrl(),
                replyText,
                imageUrls,
                isPublic
        );
        castRepository.addReply(castId, reply);
        eventPublisher.publishEvent(new CastRepliedEvent(castId,cast.getCreatorId(), replierId));

    }

    @Transactional
    public void updateReply(Long requesterId, String castId, String replyId, String replyText,List<String> imageUrls) {
        Cast cast = getCastOrThrow(castId);
        cast.updateReply(requesterId, replyId,replyText, imageUrls);
        castRepository.updateReply(castId, replyId, replyText, imageUrls);
    }

    @Transactional
    public void deleteReply(Long requesterId, String castId, String replyId) {
        Cast cast = getCastOrThrow(castId);
        cast.validateReplyDeletion(requesterId, replyId);
        castRepository.removeReply(castId, replyId);
    }

    @Transactional
    public void deleteCast(Long requesterId, String castId) {
        Cast cast = getCastOrThrow(castId);

        if (!cast.getCreatorId().equals(requesterId)) {
            throw new CastAccessDeniedException("캐스트 삭제 권한이 없습니다.");
        }

        castRepository.deleteById(castId);
    }

    @Transactional(readOnly = true)
    public Slice<Cast> getReceivedCasts(Long userId, Pageable pageable) {
        Set<Long> excludedIds = castSocialPort.getNonReceivableFriendIds(userId);
        return castRepository.findAllByRecipientId(userId, excludedIds, pageable);
    }

    @Transactional
    public Cast getCastDetail(Long userId, String castId) {
        Cast cast = getCastOrThrow(castId);
        if (!cast.isRecipient(userId) && !cast.getCreatorId().equals(userId)) {
            throw new CastAccessDeniedException("접근 권한이 없는 캐스트입니다.");
        }
        cast.markAsRead(userId);
        return castRepository.save(cast);
    }

    @Transactional
    public List<Long> getUnreadSenderIds(Long userId) {
        Set<Long> excludedIds = castSocialPort.getNonReceivableFriendIds(userId);
        return castRepository.findUnreadSenderIds(userId, excludedIds);
    }

    private Cast getCastOrThrow(String castId) {
        Cast cast = castRepository.findById(castId);
        if (cast == null) {
            throw new CastNotFoundException();
        }
        return cast;
    }
}