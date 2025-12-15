package com.example.GooRoomBe.social.trace.domain;

import java.util.Optional;

public interface TracePort {
    Optional<Trace> findTrace(String visitorId, String targetId);

    int getVisitCount(String visitorId, String targetId);

    void save(Trace trace, String visitorId);
}