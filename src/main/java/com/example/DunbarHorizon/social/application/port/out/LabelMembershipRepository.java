package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;

import java.util.Optional;

public interface LabelMembershipRepository extends LabelRepository {
    Optional<Long> findOwnerId(String labelId);
    Optional<Long> lockOwnerId(String labelId);
    void removeMembersBetween(Long userId, Long friendId);
}
