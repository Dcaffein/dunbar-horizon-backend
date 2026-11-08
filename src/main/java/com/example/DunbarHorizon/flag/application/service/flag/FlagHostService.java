package com.example.DunbarHorizon.flag.application.service.flag;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;import com.example.DunbarHorizon.flag.application.port.in.command.FlagEncoreCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;
import com.example.DunbarHorizon.flag.application.port.in.FlagHostUseCase;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagEncoreManager;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
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
    private final FlagEncoreManager encoreManager;

    @Override
    public Long hostFlag(FlagHostCommand command) {
        FlagSchedule schedule = FlagSchedule.of(command.deadline(), command.startDateTime(), command.endDateTime());
        Flag flag = Flag.create(command.hostId(), command.title(), command.description(), command.capacity(), schedule);
        return flagRepository.save(flag).getId();
    }

    @Override
    public Long encoreFlag(FlagEncoreCommand command) {
        try {
            return encoreManager.encore(
                    command.parentFlagId(),
                    command.hostId(),
                    command.deadline(),
                    command.startDateTime(),
                    command.endDateTime()).getId();
        } catch (DataIntegrityViolationException e) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_ENCORE_ALREADY_EXISTS,
                    ErrorContext.of("parentFlagId", command.parentFlagId()));
        }
    }
}
