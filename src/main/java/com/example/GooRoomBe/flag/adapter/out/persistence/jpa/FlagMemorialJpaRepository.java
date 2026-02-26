package com.example.GooRoomBe.flag.adapter.out.persistence.jpa;

import com.example.GooRoomBe.flag.domain.memorial.FlagMemorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FlagMemorialJpaRepository extends JpaRepository<FlagMemorial, Long> {

    boolean existsByFlagId(Long flagId);

    @Query("SELECT fm FROM FlagMemorial fm " +
            "WHERE fm.flagId = :flagId " +
            "AND EXISTS (" +
            "    SELECT 1 FROM FlagMemorial sub " +
            "    WHERE sub.flagId = :flagId AND sub.writerId = :viewerId" +
            ")")
    List<FlagMemorial> findAllMemorialsIfMemorialized(
            @Param("flagId") Long flagId,
            @Param("viewerId") Long viewerId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FlagMemorial fm WHERE fm.flagId IN :flagIds")
    void hardDeleteByFlagIdsIn(@Param("flagIds") Collection<Long> flagIds);
}