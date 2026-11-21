package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.domain.BuzzAudienceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuzzAudienceMemberJpaRepository extends JpaRepository<BuzzAudienceMember, BuzzAudienceMember.Key> {

    Optional<BuzzAudienceMember> findByBuzzIdAndUserId(String buzzId, Long userId);

    @Query("SELECT m.userId FROM BuzzAudienceMember m WHERE m.buzzId = :buzzId ORDER BY m.userId")
    List<Long> findUserIdsByBuzzId(@Param("buzzId") String buzzId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BuzzAudienceMember m WHERE m.buzzId = :buzzId")
    int deleteAllByBuzzId(@Param("buzzId") String buzzId);
}
