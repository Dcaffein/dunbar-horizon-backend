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

    private List<BuzzComment> comments = new ArrayList<>();

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
            throw new BuzzAccessDeniedException("해당 Buzz의 수신자가 아닙니다.");
        }
        if (!readRecipientIds.contains(userId)) {
            this.readRecipientIds.add(userId);
        }
    }

    public BuzzComment createComment(Long commenterId, String nickname, String profileImageUrl,
                                     String text, List<String> imageUrls, boolean isPublic) {
        if (isExpired()) {
            throw new BuzzInvalidStateException("만료된 Buzz에는 댓글을 남길 수 없습니다.");
        }
        if (!isRecipient(commenterId) && !creatorId.equals(commenterId)) {
            throw new BuzzAccessDeniedException("이 Buzz에 댓글을 남길 권한이 없습니다.");
        }

        if (isRecipient(commenterId)) {
            markAsRead(commenterId);
        }

        return BuzzComment.builder()
                .commentId(UUID.randomUUID().toString())
                .commenterId(commenterId)
                .commenterNickname(nickname)
                .commenterProfileImageUrl(profileImageUrl)
                .text(text)
                .imageUrls(imageUrls)
                .createdAt(LocalDateTime.now())
                .isPublic(isPublic)
                .build();
    }

    public void updateComment(Long requesterId, String commentId, String newText, List<String> newImageUrls) {
        if (isExpired()) {
            throw new BuzzInvalidStateException("만료된 Buzz의 댓글은 수정할 수 없습니다.");
        }

        BuzzComment target = findComments(commentId);

        if (!target.getCommenterId().equals(requesterId)) {
            throw new BuzzAccessDeniedException("댓글 수정 권한이 없습니다.");
        }

        target.update(newText, newImageUrls);
    }

    public void validateCommentDeletion(Long requesterId, String commentId) {
        BuzzComment target = findComments(commentId);

        if (!target.getCommenterId().equals(requesterId) && !this.creatorId.equals(requesterId)) {
            throw new BuzzAccessDeniedException("댓글 삭제 권한이 없습니다.");
        }
    }

    private BuzzComment findComments(String commentId) {
        return comments.stream()
                .filter(c -> c.getCommentId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new BuzzInvalidStateException("존재하지 않는 댓글입니다."));
    }

    private void validateRecipientIds(List<Long> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            throw new BuzzInvalidStateException("수신자가 최소 한 명 이상 지정되어야 합니다.");
        }
        if (recipientIds.size() > 150) {
            throw new BuzzInvalidStateException("수신자는 최대 150명까지 지정할 수 있습니다.");
        }
    }
}
