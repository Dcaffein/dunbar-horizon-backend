package com.example.GooRoomBe.flag.application.service.flag;

import com.example.GooRoomBe.flag.application.port.in.FlagParticipationUseCase;
import com.example.GooRoomBe.flag.domain.flag.*;
import com.example.GooRoomBe.flag.domain.flag.exception.FlagNotFoundException;
import com.example.GooRoomBe.flag.domain.flag.exception.FlagParticipantNotFoundException;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagParticipationService implements FlagParticipationUseCase {
    private final FlagRepository flagRepository;
    private final FlagParticipantRepository participantRepository;
    private final FlagParticipationPolicy flagParticipationPolicy;

    @Override
    public void participateInFlag(Long flagId, Long userId) {
        Flag flag = flagRepository.findByIdExclusive(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        FlagParticipant newParticipant = flagParticipationPolicy.participate(flag, userId);
        participantRepository.save(newParticipant);
    }

    @Override
    public void leaveFlag(Long flagId, Long userId) {
        Flag flag = flagRepository.findById(flagId).orElseThrow(() -> new FlagNotFoundException(flagId));
        FlagParticipant participant = participantRepository.findByFlagIdAndParticipantId(flagId, userId)
                .orElseThrow(() -> new FlagParticipantNotFoundException(userId));
        DeletableParticipant ticket = flag.unparticipate(participant, userId);
        participantRepository.delete(ticket);
    }
}
