package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult;
import com.example.DunbarHorizon.social.application.port.out.SocialConnectionPathRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.springframework.transaction.annotation.Transactional;

public class MySqlSocialConnectionPathQueryService extends SocialConnectionPathQueryService {

    public MySqlSocialConnectionPathQueryService(SocialConnectionPathRepository connectionPathRepository,
                                               FriendshipRepository friendshipRepository) {
        super(connectionPathRepository, friendshipRepository);
    }

    @Override
    @Transactional(transactionManager = "transactionManager", readOnly = true)
    public ConnectionPathResult getConnectionPath(Long myId, Long targetId) {
        return super.getConnectionPath(myId, targetId);
    }
}
