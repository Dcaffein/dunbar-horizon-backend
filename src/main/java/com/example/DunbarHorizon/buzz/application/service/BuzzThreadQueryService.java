package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.dto.info.BuzzAudienceInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadDetailInfo;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzThreadDetailInfo.ThreadContent;
import com.example.DunbarHorizon.buzz.application.dto.info.BuzzUserProfileInfo;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzCommentResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzProfileResult;
import com.example.DunbarHorizon.buzz.application.dto.result.BuzzThreadDetailResult;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzQueryRepository;
import com.example.DunbarHorizon.buzz.application.port.out.BuzzSocialPort;
import com.example.DunbarHorizon.buzz.application.port.out.ImageStoragePort;
import com.example.DunbarHorizon.buzz.domain.Buzz;
import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzNotFoundException;
import com.example.DunbarHorizon.buzz.domain.exception.BuzzThreadNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuzzThreadQueryService {

    private final BuzzQueryRepository buzzQueryRepository;
    private final BuzzSocialPort buzzSocialPort;
    private final BuzzProfileAssembler buzzProfileAssembler;
    private final ImageStoragePort imageStoragePort;

    public BuzzThreadDetailResult getThread(Long viewerId, Long buzzId, Long threadId) {
        BuzzThreadDetailInfo buzz = buzzQueryRepository.findThreadDetail(buzzId, threadId)
                .orElseThrow(() -> new BuzzNotFoundException(buzzId));
        List<BuzzAudienceInfo> audience = buzzQueryRepository.findAudience(buzzId);
        boolean isAudienceMember = audience.stream().anyMatch(member -> member.userId().equals(viewerId));
        Buzz.validateAccess(buzzId, buzz.creatorId(), viewerId, isAudienceMember);

        ThreadContent thread = Optional.ofNullable(buzz.thread())
                .orElseThrow(() -> new BuzzThreadNotFoundException(buzzId, threadId));
        Set<Long> starterFriendIds = starterFriendIdsAmong(buzz, thread, audience);
        BuzzThread.validateAccess(threadId, thread.visibility(), thread.starterId(), buzz.creatorId(),
                viewerId, starterFriendIds.contains(viewerId));

        return BuzzThreadDetailResult.of(
                buzzId,
                thread,
                buzzProfileAssembler.profileOf(thread.starterId(), thread.starterProfile()),
                imageStoragePort.resolveUrls(thread.imageKeys()),
                viewerId,
                viewersOf(buzz, thread, audience, starterFriendIds),
                buzzQueryRepository.findComments(threadId).stream()
                        .map(comment -> BuzzCommentResult.of(
                                comment,
                                buzzProfileAssembler.profileOf(comment.authorId(), comment.authorProfile()),
                                imageStoragePort.resolveUrls(comment.imageKeys()),
                                viewerId))
                        .toList());
    }

    private Set<Long> starterFriendIdsAmong(BuzzThreadDetailInfo buzz, ThreadContent thread,
                                            List<BuzzAudienceInfo> audience) {
        Set<Long> candidateIds = audience.stream()
                .map(BuzzAudienceInfo::userId)
                .filter(userId -> BuzzThread.needsFriendshipToAccess(
                        thread.visibility(), thread.starterId(), buzz.creatorId(), userId))
                .collect(Collectors.toSet());
        return candidateIds.isEmpty() ? Set.of() : buzzSocialPort.getFriendIdsIn(thread.starterId(), candidateIds);
    }

    private List<BuzzProfileResult> viewersOf(BuzzThreadDetailInfo buzz, ThreadContent thread,
                                              List<BuzzAudienceInfo> audience, Set<Long> starterFriendIds) {
        Map<Long, BuzzUserProfileInfo> candidates = new LinkedHashMap<>();
        candidates.put(buzz.creatorId(), buzz.creatorProfile());
        candidates.put(thread.starterId(), thread.starterProfile());
        audience.forEach(member -> candidates.putIfAbsent(member.userId(), member.profile()));

        return candidates.entrySet().stream()
                .filter(candidate -> candidate.getValue() != null)
                .filter(candidate -> BuzzThread.isAccessibleBy(thread.visibility(), thread.starterId(),
                        buzz.creatorId(), candidate.getKey(), starterFriendIds.contains(candidate.getKey())))
                .map(candidate -> buzzProfileAssembler.profileOf(candidate.getKey(), candidate.getValue()))
                .toList();
    }
}
