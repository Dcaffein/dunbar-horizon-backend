package com.example.DunbarHorizon.social.application.network.port;

import com.example.DunbarHorizon.social.application.network.dto.ConnectionPathResult;

public interface SocialConnectionPathRepository {
    ConnectionPathResult.Intermediaries findIntermediaries(Long myId, Long targetId, int limit);
}
