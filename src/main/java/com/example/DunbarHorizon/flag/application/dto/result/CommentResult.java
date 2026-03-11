package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.domain.comment.FlagComment;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResult(
        Long id,
        WriterInfo writerInfo,
        String content,
        boolean isPrivate,
        LocalDateTime createdAt,
        List<CommentResult> replies
) {
    public static CommentResult of(FlagComment comment, WriterInfo writerInfo, String content, List<CommentResult> replies) {
        return new CommentResult(
                comment.getId(),
                writerInfo,
                content,
                comment.isPrivate(),
                comment.getCreatedAt(),
                replies
        );
    }

    public record WriterInfo(
            Long id,
            String nickname,
            String profileImageUrl
    ) {
        public static WriterInfo from(FlagUserInfo info) {
            return new WriterInfo(
                    info.userId(),
                    info.nickname(),
                    info.profileImageUrl()
            );
        }
    }
}
