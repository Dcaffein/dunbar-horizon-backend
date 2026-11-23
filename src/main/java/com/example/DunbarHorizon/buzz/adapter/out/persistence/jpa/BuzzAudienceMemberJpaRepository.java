package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuzzAudienceMemberJpaRepository extends JpaRepository<BuzzAudienceMember, BuzzAudienceMember.Key> {

    Optional<BuzzAudienceMember> findByBuzzIdAndUserId(Long buzzId, Long userId);
}
