package com.example.DunbarHorizon.flag.application.dto.result;

import java.util.List;

public record MemorialListResult(
        List<MemorialResult> memorials,
        boolean locked
) {
    public static MemorialListResult empty() {
        return new MemorialListResult(List.of(), false);
    }

    public static MemorialListResult asLocked() {
        return new MemorialListResult(List.of(), true);
    }

    public static MemorialListResult of(List<MemorialResult> memorials) {
        return new MemorialListResult(memorials, false);
    }
}
