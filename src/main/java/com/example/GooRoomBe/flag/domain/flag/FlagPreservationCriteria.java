package com.example.GooRoomBe.flag.domain.flag;

public record FlagPreservationCriteria(
        boolean hasRelatedMemorial,
        boolean hasEncore
) {
    public boolean isSatisfied() {
        return hasRelatedMemorial || hasEncore;
    }
}
