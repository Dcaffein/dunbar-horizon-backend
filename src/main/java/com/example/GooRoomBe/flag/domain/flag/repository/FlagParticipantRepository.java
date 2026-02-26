package com.example.GooRoomBe.flag.domain.flag.repository;

import com.example.GooRoomBe.flag.domain.flag.DeletableParticipant;
import com.example.GooRoomBe.flag.domain.flag.FlagParticipant;

import java.util.List;
import java.util.Optional;

public interface FlagParticipantRepository {
    FlagParticipant save(FlagParticipant participant);
    void delete(DeletableParticipant participant);
    Optional<FlagParticipant> findByFlagIdAndParticipantId(Long flagId, Long participantId);
    int countByFlagId(Long flagId);
    boolean isParticipating(Long flagId, Long participantId);
    List<Long> findAllParticipantIdsByFlagId(Long flagId);
    void deleteAllByFlagId(Long flagId);
    List<Long> findFlagIdByParticipantId(Long participantId);
    List<FlagParticipant> findAllByFlagId(Long flagId);
}