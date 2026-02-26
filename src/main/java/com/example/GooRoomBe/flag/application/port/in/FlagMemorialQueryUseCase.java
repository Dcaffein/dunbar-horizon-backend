package com.example.GooRoomBe.flag.application.port.in;

import com.example.GooRoomBe.flag.application.port.in.dto.MemorialResponse;

import java.util.List;

public interface FlagMemorialQueryUseCase {
    List<MemorialResponse> getMemorials(Long flagId, Long viewerId);
}