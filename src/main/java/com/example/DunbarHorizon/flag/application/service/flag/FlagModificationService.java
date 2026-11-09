package com.example.DunbarHorizon.flag.application.service.flag;

import com.example.DunbarHorizon.flag.application.port.in.FlagModificationUseCase;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagCapacityUpdateCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagDetailsUpdateCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagScheduleUpdateCommand;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagExpiryExemptionUpdater;
import com.example.DunbarHorizon.flag.domain.flag.FlagSchedule;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagModificationService implements FlagModificationUseCase {
    private final FlagRepository flagRepository;
    private final FlagExpiryExemptionUpdater exemptionUpdater;

    @Override
    public void modifyFlagDetails(FlagDetailsUpdateCommand command) {
        Flag flag = getFlagOrThrow(command.flagId());
        flag.updateBasicInfo(command.hostId(), command.title(), command.description());
        flagRepository.save(flag);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void modifyFlagCapacity(FlagCapacityUpdateCommand command) {
        Flag flag = flagRepository.findByIdForUpdate(command.flagId())
                .orElseThrow(() -> new FlagNotFoundException(command.flagId()));
        int currentCount = flagRepository.countParticipants(command.flagId());
        flag.updateCapacity(command.hostId(), command.capacity(), currentCount);
        flagRepository.save(flag);
    }

    @Override
    public void reschedule(FlagScheduleUpdateCommand command) {
        FlagSchedule newSchedule = FlagSchedule.of(command.deadline(), command.startDateTime(), command.endDateTime());
        Flag flag = flagRepository.findByIdForUpdate(command.flagId())
                .orElseThrow(() -> new FlagNotFoundException(command.flagId()));
        flag.reschedule(command.hostId(), newSchedule);
        flagRepository.save(flag);
    }

    @Override
    public void closeRecruitment(Long flagId, Long hostId) {
        Flag flag = flagRepository.findByIdForUpdate(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        flag.closeRecruitment(hostId);
        flagRepository.save(flag);
    }

    // 대상을 먼저 잠그는 것은 믿을 수 있는 parentId가 잠근 대상에서만 나오기 때문이다. 두 Flag를
    // 함께 잠그는 경로는 모두 대상 → 부모 순서를 따라야 교착이 없다. 두 잠금 사이에 일반 조회를
    // 넣으면 REPEATABLE READ의 읽기 시점이 부모 잠금 전에 고정되어 부모 면제 재계산이 낡는다.
    @Override
    public void closeFlag(Long flagId, Long userId) {
        Flag flag = flagRepository.findByIdForUpdate(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        Long parentId = flag.getParentId();
        flag.delete(userId);
        flagRepository.save(flag);

        if (parentId == null) return;
        flagRepository.findByIdForUpdate(parentId).ifPresent(parent -> {
            exemptionUpdater.refresh(parent);
            flagRepository.save(parent);
        });
    }

    private Flag getFlagOrThrow(Long id) {
        return flagRepository.findById(id).orElseThrow(() -> new FlagNotFoundException(id));
    }
}
