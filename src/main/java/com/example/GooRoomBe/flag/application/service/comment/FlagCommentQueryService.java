package com.example.GooRoomBe.flag.application.service.comment;

import com.example.GooRoomBe.flag.application.port.in.FlagCommentQueryUseCase;
import com.example.GooRoomBe.flag.application.port.in.dto.CommentResponse;
import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;
import com.example.GooRoomBe.flag.application.port.out.FlagUserPort;
import com.example.GooRoomBe.flag.domain.comment.FlagComment;
import com.example.GooRoomBe.flag.domain.comment.repository.FlagCommentRepository;
import com.example.GooRoomBe.flag.domain.flag.Flag;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlagCommentQueryService implements FlagCommentQueryUseCase {

    private final FlagCommentRepository commentRepository;
    private final FlagRepository flagRepository;
    private final FlagUserPort flagUserPort;

    @Override
    public List<CommentResponse> getCommentTree(Long flagId, Long viewerId) {
        Flag flag = flagRepository.findById(flagId).orElseThrow();
        List<FlagComment> allComments = commentRepository.findAllByFlagId(flagId);

        Map<Long, List<FlagComment>> childrenMap = allComments.stream()
                .filter(FlagComment::isReply)
                .collect(Collectors.groupingBy(FlagComment::getParentId));

        Map<Long, FlagUserInfo> userInfoMap = flagUserPort.findUserInfosByIds(extractUserIds(allComments, flag));
        Map<Long, Long> writerMap = allComments.stream()
                .collect(Collectors.toMap(FlagComment::getId, FlagComment::getWriterId));

        return allComments.stream()
                .filter(c -> !c.isReply())
                .filter(c -> c.isVisibleTo(viewerId, flag.getHostId(), null)) // 권한 필터링
                .map(root -> convertToResponse(root, childrenMap, flag.getHostId(), viewerId, writerMap, userInfoMap))
                .sorted(Comparator.comparing(CommentResponse::createdAt))
                .toList();
    }

    @Override
    public Long getCommentCount(Long flagId) {
        return 0L;
    }

    private CommentResponse convertToResponse(
            FlagComment current,
            Map<Long, List<FlagComment>> childrenMap,
            Long hostId,
            Long viewerId,
            Map<Long, Long> writerMap,
            Map<Long, FlagUserInfo> userInfoMap
    ) {
        CommentResponse.WriterInfo writer = CommentResponse.WriterInfo.from(userInfoMap.get(current.getWriterId()));

        List<FlagComment> children = childrenMap.getOrDefault(current.getId(), Collections.emptyList());

        List<CommentResponse> replies = children.stream()
                .filter(c -> {
                    Long parentWriterId = writerMap.get(c.getParentId());
                    return c.isVisibleTo(viewerId, hostId, parentWriterId);
                })
                .map(reply -> convertToResponse(reply, childrenMap, hostId, viewerId, writerMap, userInfoMap))
                .sorted(Comparator.comparing(CommentResponse::createdAt))
                .toList();

        return CommentResponse.of(current, writer, current.getContent(), replies);
    }

    private Set<Long> extractUserIds(List<FlagComment> allComments, Flag flag) {

        Set<Long> userIds = allComments.stream()
                .map(FlagComment::getWriterId)
                .collect(Collectors.toSet());

        userIds.add(flag.getHostId());

        return userIds;
    }
}