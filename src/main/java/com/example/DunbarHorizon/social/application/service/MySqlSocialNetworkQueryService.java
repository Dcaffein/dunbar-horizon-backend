package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import com.example.DunbarHorizon.social.domain.friend.SocialNetworkExposurePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class MySqlSocialNetworkQueryService extends SocialNetworkQueryService {

    public MySqlSocialNetworkQueryService(SocialNetworkRepository socialNetworkRepository,
                                         FriendshipRepository friendshipRepository,
                                         SocialNetworkExposurePolicy exposurePolicy) {
        super(socialNetworkRepository, friendshipRepository, exposurePolicy);
    }

    @Override
    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<MutualFriendEdgeResult> getNetworkEdges(Long userId, Long targetId, List<Long> baseNetworkFriendIds) {
        return super.getNetworkEdges(userId, targetId, baseNetworkFriendIds);
    }
}
