package com.example.DunbarHorizon.social.application.port.out;

public interface LabelMembershipRepository {
    void removeMembersBetween(Long userId, Long friendId);
}
