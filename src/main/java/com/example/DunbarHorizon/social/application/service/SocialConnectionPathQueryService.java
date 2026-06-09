package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.global.annotation.Neo4jTransactional;
import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult;
import com.example.DunbarHorizon.social.application.port.in.SocialConnectionPathQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.SocialConnectionPathRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Neo4jTransactional(readOnly = true)
public class SocialConnectionPathQueryService implements SocialConnectionPathQueryUseCase {

    private final SocialConnectionPathRepository connectionPathRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    public ConnectionPathResult getConnectionPath(Long myId, Long targetId) {
        if (myId.equals(targetId)) {
            return new ConnectionPathResult(false, List.of());
        }
        boolean direct = friendshipRepository.existsFriendshipBetween(myId, targetId);
        List<ConnectionPathResult.IntermediaryResult> intermediaries =
                connectionPathRepository.findIntermediaries(myId, targetId);
        return new ConnectionPathResult(direct, intermediaries);
    }
}
