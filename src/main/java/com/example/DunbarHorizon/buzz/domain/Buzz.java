package com.example.DunbarHorizon.buzz.domain;

import com.example.DunbarHorizon.buzz.domain.exception.BuzzAccessDeniedException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzInvalidStateException;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "buzzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Buzz {

    @Id
    private String id;

    @Indexed
    private Long creatorId;

    private String creatorNickname;

    private String creatorProfileImageUrl;

    private String text;

    private List<String> imageUrls;

    @Indexed
    private List<Long> recipientIds = new ArrayList<>();

    private List<Long> readRecipientIds = new ArrayList<>();

    private List<BuzzReply> replies = new ArrayList<>();

    private LocalDateTime createdAt;

    @Indexed(expireAfter = "0s")
    private LocalDateTime expiresAt;

    @Builder
    public Buzz(Long creatorId, String creatorNickname, String creatorProfileImageUrl,
                String text, List<String> imageUrls, List<Long> recipientIds) {
        validateRecipientIds(recipientIds);

        this.creatorId = creatorId;
        this.creatorNickname = creatorNickname;
        this.creatorProfileImageUrl = creatorProfileImageUrl;
        this.text = text;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.recipientIds = recipientIds;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusMinutes(30);
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isRecipient(Long userId) {
        return recipientIds.contains(userId);
    }

    public boolean isUnreadBy(Long userId) {
        return isRecipient(userId) && !readRecipientIds.contains(userId);
    }

    public void markAsRead(Long userId) {
        if (!isRecipient(userId)) {
            throw new BuzzAccessDeniedException("해당 캐스트의 수신자가 아닙니다.");
        }
        if (!readRecipientIds.contains(userId)) {
            this.readRecipientIds.add(userId);
        }
    }

    public BuzzReply createReply(Long replierId, String nickname, String profileImageUrl,
                                 String text, List<String> imageUrls, boolean isPublic) {
        if (isExpired()) {
            throw new BuzzInvalidStateException("만료된 캐스트에는 답장할 수 없습니다.");
        }
        if (!isRecipient(replierId)) {
            throw new BuzzAccessDeniedException("이 캐스트에 답장을 남길 권한이 없습니다.");
        }

        markAsRead(replierId);

        return BuzzReply.builder()
                .replyId(UUID.randomUUID().toString())
                .replierId(replierId)
                .replierNickname(nickname)
                .replierProfileImageUrl(profileImageUrl)
                .text(text)
                .imageUrls(imageUrls)
                .createdAt(LocalDateTime.now())
                .isPublic(isPublic)
                .build();
    }

    public void updateReply(Long requesterId, String replyId, String newText, List<String> newImageUrls) {
        if (isExpired()) {
            throw new BuzzInvalidStateException("만료된 캐스트의 답장은 수정할 수 없습니다.");
        }

        BuzzReply target = findReplies(replyId);

        if (!target.getReplierId().equals(requesterId)) {
            throw new BuzzAccessDeniedException("답장 수정 권한이 없습니다.");
        }

        target.update(newText, newImageUrls);
    }

    public void validateReplyDeletion(Long requesterId, String replyId) {
        BuzzReply target = findReplies(replyId);

        if (!target.getReplierId().equals(requesterId) && !this.creatorId.equals(requesterId)) {
            throw new BuzzAccessDeniedException("답장 삭제 권한이 없습니다.");
        }
    }

    private BuzzReply findReplies(String replyId) {
        return replies.stream()
                .filter(r -> r.getReplyId().equals(replyId))
                .findFirst()
                .orElseThrow(() -> new BuzzInvalidStateException("존재하지 않는 답장입니다."));
    }

    private void validateRecipientIds(List<Long> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            throw new BuzzInvalidStateException("수신자가 최소 한 명 이상 지정되어야 합니다.");
        }
    }
}