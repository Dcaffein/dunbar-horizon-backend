package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.result.ConnectionPathResult;

import java.util.List;

public interface SocialConnectionPathRepository {
    List<ConnectionPathResult.IntermediaryResult> findIntermediaries(Long myId, Long targetId);
}
