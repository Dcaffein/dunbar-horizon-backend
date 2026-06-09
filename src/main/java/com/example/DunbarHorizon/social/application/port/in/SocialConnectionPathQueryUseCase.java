package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult;

public interface SocialConnectionPathQueryUseCase {
    ConnectionPathResult getConnectionPath(Long myId, Long targetId);
}
