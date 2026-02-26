package com.example.GooRoomBe.flag.application.service.flag;

import com.example.GooRoomBe.flag.application.command.FlagEncoreCommand;
import com.example.GooRoomBe.flag.application.command.FlagHostCommand;
import com.example.GooRoomBe.flag.application.port.in.FlagHostUseCase;
import com.example.GooRoomBe.flag.domain.flag.Flag;
import com.example.GooRoomBe.flag.domain.flag.FlagEncoreCreator;
import com.example.GooRoomBe.flag.domain.flag.FlagSchedule;
import com.example.GooRoomBe.flag.domain.flag.exception.FlagNotFoundException;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagHostService implements FlagHostUseCase {
    private final FlagRepository flagRepository;
    private final FlagEncoreCreator flagEncoreCreator;

    @Override
    public Long hostFlag(FlagHostCommand command) {
        FlagSchedule schedule = FlagSchedule.of(command.deadline(), command.startDateTime(), command.endDateTime());
        Flag flag = Flag.create(command.hostId(), command.title(), command.description(), command.capacity(), schedule);
        return flagRepository.save(flag).getId();
    }

    @Override
    public Long encoreFlag(FlagEncoreCommand command) {
        Flag parentFlag = flagRepository.findById(command.parentFlagId())
                .orElseThrow(() -> new FlagNotFoundException(command.parentFlagId()));
        Flag encoreFlag = flagEncoreCreator.encore(
                parentFlag,
                command.hostId(),
                command.deadline(),
                command.startDateTime(),
                command.endDateTime());
        return flagRepository.save(encoreFlag).getId();
    }
}
