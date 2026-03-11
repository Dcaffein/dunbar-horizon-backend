package com.example.DunbarHorizon.flag.application.service.comment;

import com.example.DunbarHorizon.flag.application.port.in.FlagCommentQueryUseCase;
import com.example.DunbarHorizon.flag.application.dto.result.CommentResult;
import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.application.port.out.FlagUserPort;
import com.example.DunbarHorizon.flag.domain.comment.FlagComment;
import com.example.DunbarHorizon.flag.domain.comment.repository.FlagCommentRepository;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
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
    public List<CommentResult> getCommentTree(Long flagId, Long viewerId) {
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
                .sorted(Comparator.comparing(CommentResult::createdAt))
                .toList();
    }

    @Override
    public Long getCommentCount(Long flagId) {
        return 0L;
    }

    private CommentResult convertToResponse(
            FlagComment current,
            Map<Long, List<FlagComment>> childrenMap,
            Long hostId,
            Long viewerId,
            Map<Long, Long> writerMap,
            Map<Long, FlagUserInfo> userInfoMap
    ) {
        CommentResult.WriterInfo writer = CommentResult.WriterInfo.from(userInfoMap.get(current.getWriterId()));

        List<FlagComment> children = childrenMap.getOrDefault(current.getId(), Collections.emptyList());

        List<CommentResult> replies = children.stream()
                .filter(c -> {
                    Long parentWriterId = writerMap.get(c.getParentId());
                    return c.isVisibleTo(viewerId, hostId, parentWriterId);
                })
                .map(reply -> convertToResponse(reply, childrenMap, hostId, viewerId, writerMap, userInfoMap))
                .sorted(Comparator.comparing(CommentResult::createdAt))
                .toList();

        return CommentResult.of(current, writer, current.getContent(), replies);
    }

    private Set<Long> extractUserIds(List<FlagComment> allComments, Flag flag) {

        Set<Long> userIds = allComments.stream()
                .map(FlagComment::getWriterId)
                .collect(Collectors.toSet());

        userIds.add(flag.getHostId());

        return userIds;
    }
}