package com.example.DunbarHorizon.buzz.application.dto.result;

import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.ThreadVisibility;

import java.time.LocalDateTime;
import java.util.List;

public record BuzzThreadDetailResult(
        Long threadId,
        Long buzzId,
        BuzzProfileResult starter,
        ThreadVisibility visibility,
        String text,
        List<String> imageUrls,
        LocalDateTime createdAt,
        boolean isMine,
        List<BuzzProfileResult> viewers,
        List<BuzzCommentResult> comments
) {
    public static BuzzThreadDetailResult of(BuzzThread thread, BuzzProfileResult starter, List<String> imageUrls,
                                            Long viewerId, List<BuzzProfileResult> viewers,
                                            List<BuzzCommentResult> comments) {
        return new BuzzThreadDetailResult(
                thread.getId(),
                thread.getBuzzId(),
                starter,
                thread.getVisibility(),
                thread.getText(),
                imageUrls,
                thread.getCreatedAt(),
                thread.isStarter(viewerId),
                viewers,
                comments
        );
    }
}
