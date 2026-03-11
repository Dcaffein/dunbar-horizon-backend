package com.example.DunbarHorizon.flag.domain.flag;

public record FlagPreservationCriteria(
        boolean hasRelatedMemorial,
        boolean hasEncore
) {
    public boolean isSatisfied() {
        return hasRelatedMemorial || hasEncore;
    }
}
