package com.example.GooRoomBe.social.application.port.in;

import com.example.GooRoomBe.social.application.port.in.dto.LabelResponse;

import java.util.List;
import java.util.Set;

public interface LabelQueryUseCase {
    Set<Long> getMemberIdsByLabels(Long ownerId, List<String> labelIds);
    List<LabelResponse> getAllLabels(Long ownerId);
}