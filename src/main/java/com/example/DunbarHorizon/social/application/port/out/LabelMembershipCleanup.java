package com.example.DunbarHorizon.social.application.port.out;

public interface LabelMembershipCleanup {
    void removeMembershipsBetween(Long firstUserId, Long secondUserId);
}
