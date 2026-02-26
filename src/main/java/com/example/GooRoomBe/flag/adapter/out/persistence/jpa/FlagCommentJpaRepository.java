package com.example.GooRoomBe.flag.adapter.out.persistence.jpa;

import com.example.GooRoomBe.flag.domain.comment.FlagComment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FlagCommentJpaRepository extends JpaRepository<FlagComment, Long> {

    List<FlagComment> findAllByFlagId(Long flagId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM FlagComment c WHERE c.id = :id")
    Optional<FlagComment> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FlagComment c WHERE c.id = :id OR c.parentId = :id")
    void deleteTargetAndReplies(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM FlagComment c WHERE c.flagId IN :flagIds")
    void hardDeleteByFlagIdsIn(@Param("flagIds") Collection<Long> flagIds);
}