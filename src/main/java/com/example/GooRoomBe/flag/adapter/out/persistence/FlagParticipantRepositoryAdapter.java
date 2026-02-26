package com.example.GooRoomBe.flag.adapter.out.persistence;

import com.example.GooRoomBe.flag.adapter.out.persistence.jpa.FlagParticipantJpaRepository;
import com.example.GooRoomBe.flag.domain.flag.DeletableParticipant;
import com.example.GooRoomBe.flag.domain.flag.FlagParticipant;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FlagParticipantRepositoryAdapter implements FlagParticipantRepository {

    private final FlagParticipantJpaRepository jpaRepository;

    @Override
    public FlagParticipant save(FlagParticipant participant) {
        return jpaRepository.save(participant);
    }

    @Override
    public void delete(DeletableParticipant participant) {
        jpaRepository.delete(participant.getEntity());
    }

    @Override
    public Optional<FlagParticipant> findByFlagIdAndParticipantId(Long flagId, Long participantId) {
        return jpaRepository.findByFlagIdAndParticipantId(flagId, participantId);
    }

    @Override
    public int countByFlagId(Long flagId) {
        return jpaRepository.countByFlagId(flagId);
    }

    @Override
    public boolean isParticipating(Long flagId, Long participantId) {
        return jpaRepository.existsByFlagIdAndParticipantId(flagId, participantId);
    }

    @Override
    public List<Long> findAllParticipantIdsByFlagId(Long flagId) {
        return jpaRepository.findAllParticipantIdsByFlagId(flagId);
    }

    @Override
    public void deleteAllByFlagId(Long flagId) {
        jpaRepository.deleteAllByFlagId(flagId);
    }

    @Override
    public List<Long> findFlagIdByParticipantId(Long participantId) {
        return jpaRepository.findFlagIdByParticipantId(participantId);
    }

    @Override
    public List<FlagParticipant> findAllByFlagId(Long flagId) {
        return jpaRepository.findAllByFlagId(flagId);
    }
}