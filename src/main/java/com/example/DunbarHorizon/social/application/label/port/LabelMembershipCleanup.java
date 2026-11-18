package com.example.DunbarHorizon.social.application.label.port;

public interface LabelMembershipCleanup {
    void removeMembershipsBetween(Long firstUserId, Long secondUserId);
}
