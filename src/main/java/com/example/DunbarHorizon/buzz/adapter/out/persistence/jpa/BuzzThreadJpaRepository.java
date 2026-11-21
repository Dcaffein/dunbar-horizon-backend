package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.domain.BuzzThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BuzzThreadJpaRepository extends JpaRepository<BuzzThread, Long> {

    List<BuzzThread> findAllByBuzzIdOrderByCreatedAtAscIdAsc(String buzzId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BuzzThread t WHERE t.buzzId = :buzzId")
    int deleteAllByBuzzId(@Param("buzzId") String buzzId);
}
