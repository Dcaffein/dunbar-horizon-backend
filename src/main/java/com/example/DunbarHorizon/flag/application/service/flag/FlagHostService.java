package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.port.in.command.FlagEncoreCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagEncoreFactory;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagHostService implements FlagHostUseCase {
    private final FlagRepository flagRepository;
    private final FlagEncoreFactory flagEncoreFactory;

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
        Flag encoreFlag = flagEncoreFactory.encore(
                parentFlag,
                command.hostId(),
                command.deadline(),
                command.startDateTime(),
                command.endDateTime());
        try {
            return flagRepository.save(encoreFlag).getId();
        } catch (DataIntegrityViolationException e) {
            throw new FlagInvalidStatusException("이미 앵콜이 존재하는 플래그입니다.");
        }
    }
}
