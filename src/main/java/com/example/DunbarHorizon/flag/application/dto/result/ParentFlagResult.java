package com.example.DunbarHorizon.flag.application.dto.result;

import com.example.DunbarHorizon.flag.domain.flag.Flag;

public record ParentFlagResult(Long id, String title) {
    public static ParentFlagResult from(Flag flag) {
        return new ParentFlagResult(flag.getId(), flag.getTitle());
    }
}
