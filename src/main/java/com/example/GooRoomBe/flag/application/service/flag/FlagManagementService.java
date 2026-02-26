package com.example.GooRoomBe.flag.application.service.flag;

import com.example.GooRoomBe.flag.application.command.*;
import com.example.GooRoomBe.flag.application.port.in.FlagManagementUseCase;
import com.example.GooRoomBe.flag.domain.flag.Flag;
import com.example.GooRoomBe.flag.domain.flag.FlagParticipationPolicy;
import com.example.GooRoomBe.flag.domain.flag.FlagSchedule;
import com.example.GooRoomBe.flag.domain.flag.exception.FlagNotFoundException;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagManagementService implements FlagManagementUseCase {
    private final FlagRepository flagRepository;
    private final FlagParticipationPolicy flagParticipationPolicy;

    @Override
    public void modifyFlagDetails(FlagDetailsUpdateCommand command) {
        Flag flag = getFlagOrThrow(command.flagId());
        flag.updateBasicInfo(command.hostId(), command.title(), command.description());
    }

    @Override
    public void modifyFlagCapacity(FlagCapacityUpdateCommand command) {
        Flag flag = flagRepository.findByIdExclusive(command.flagId())
                .orElseThrow(() -> new FlagNotFoundException(command.flagId()));
        flagParticipationPolicy.updateCapacity(flag, command.hostId(), command.capacity());
    }

    @Override
    public void reschedule(FlagScheduleUpdateCommand command) {
        FlagSchedule newSchedule = FlagSchedule.of(command.deadline(), command.startDateTime(), command.endDateTime());
        getFlagOrThrow(command.flagId()).reschedule(command.hostId(), newSchedule);
    }

    @Override
    public void closeRecruitment(Long flagId, Long hostId) {
        Flag flag = flagRepository.findByIdExclusive(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        flag.closeRecruitment(hostId);
    }

    @Override
    public void closeFlag(Long flagId, Long userId) {
        Flag flag = getFlagOrThrow(flagId);
        flag.delete(userId);
        flagRepository.save(flag);
    }

    private Flag getFlagOrThrow(Long id) {
        return flagRepository.findById(id).orElseThrow(() -> new FlagNotFoundException(id));
    }
}