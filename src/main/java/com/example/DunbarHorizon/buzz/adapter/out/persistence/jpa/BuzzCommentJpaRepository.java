package com.example.DunbarHorizon.buzz.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.buzz.domain.BuzzComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BuzzCommentJpaRepository extends JpaRepository<BuzzComment, Long> {

    List<BuzzComment> findAllByThreadIdOrderByIdAsc(Long threadId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BuzzComment c WHERE c.id = :id")
    void deleteById(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BuzzComment c WHERE c.threadId = :threadId")
    int deleteAllByThreadId(@Param("threadId") Long threadId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BuzzComment c WHERE c.threadId IN (SELECT t.id FROM BuzzThread t WHERE t.buzzId = :buzzId)")
    int deleteAllByThreadBuzzId(@Param("buzzId") String buzzId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BuzzComment c WHERE c.threadId IN (SELECT t.id FROM BuzzThread t WHERE t.buzzId IN :buzzIds)")
    int deleteAllByThreadBuzzIdIn(@Param("buzzIds") Collection<String> buzzIds);
}
