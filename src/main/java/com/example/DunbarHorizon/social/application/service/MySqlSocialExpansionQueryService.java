package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.AnchorExpansionResult;
import com.example.DunbarHorizon.social.application.port.out.SocialExpansionRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class MySqlSocialExpansionQueryService extends SocialExpansionQueryService {

    public MySqlSocialExpansionQueryService(SocialExpansionRepository socialExpansionRepository,
                                           FriendshipRepository friendshipRepository) {
        super(socialExpansionRepository, friendshipRepository);
    }

    @Override
    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<AnchorExpansionResult> getAnchorExpansion(Long userId, Long anchorFriendId, Double expansionValue) {
        return super.getAnchorExpansion(userId, anchorFriendId, expansionValue);
    }

    @Override
    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public List<AnchorExpansionResult> getRecommendationsByAnchor(Long userId, Long anchorFriendId) {
        return super.getRecommendationsByAnchor(userId, anchorFriendId);
    }
}
