package com.example.GooRoomBe.flag.application.port.in.dto;

import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;
import com.example.GooRoomBe.flag.domain.comment.FlagComment;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        WriterInfo writerInfo,
        String content,
        boolean isPrivate,
        LocalDateTime createdAt,
        List<CommentResponse> replies
) {
    public static CommentResponse of(FlagComment comment, WriterInfo writerInfo, String content, List<CommentResponse> replies) {
        return new CommentResponse(
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