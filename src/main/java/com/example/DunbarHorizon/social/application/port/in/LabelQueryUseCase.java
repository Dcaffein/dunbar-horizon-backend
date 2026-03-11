package com.example.DunbarHorizon.social.application.port.in;

import com.example.DunbarHorizon.social.application.dto.result.LabelResult;

import java.util.List;
import java.util.Set;

public interface LabelQueryUseCase {
    Set<Long> getMemberIdsByLabels(Long ownerId, List<String> labelIds);
    List<LabelResult> getAllLabels(Long ownerId);
}