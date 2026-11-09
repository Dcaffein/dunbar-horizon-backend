package com.example.DunbarHorizon.flag.application.service.memorial;

import com.example.DunbarHorizon.flag.application.port.in.FlagMemorialCommandUseCase;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagExpiryExemptionUpdater;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialNotFoundException;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagMemorialCommandService implements FlagMemorialCommandUseCase {

    private final FlagRepository flagRepository;
    private final FlagMemorialRepository memorialRepository;
    private final FlagExpiryExemptionUpdater exemptionUpdater;

    @Override
    public Long createMemorial(Long flagId, Long userId, String content) {
        Flag flag = lockFlag(flagId);
        boolean isParticipant = !flag.getHostId().equals(userId)
                && flagRepository.isParticipating(flagId, userId);
        FlagMemorial memorial = memorialRepository.save(
                FlagMemorial.create(flag, userId, content, isParticipant));
        refreshExemption(flag);
        return memorial.getId();
    }

    @Override
    public void updateMemorial(Long flagId, Long memorialId, Long requesterId, String content) {
        FlagMemorial memorial = memorialRepository.findById(memorialId)
                .orElseThrow(() -> new FlagMemorialNotFoundException(memorialId));
        validateBelongsToFlag(memorial, flagId);

        memorial.updateContent(requesterId, content);
        memorialRepository.save(memorial);
    }

    @Override
    public void deleteMemorial(Long flagId, Long memorialId, Long requesterId) {
        Flag flag = lockFlag(flagId);
        FlagMemorial memorial = memorialRepository.findById(memorialId)
                .filter(found -> flagId.equals(found.getFlagId()))
                .orElseThrow(() -> new FlagMemorialNotFoundException(memorialId));
        memorial.validateDeletion(requesterId);
        memorialRepository.delete(memorial);
        refreshExemption(flag);
    }

    // 생성·삭제 트랜잭션의 첫 조회여야 한다. 일반 조회가 앞서면 REPEATABLE READ의
    // 읽기 시점이 고정되어 잠금 대기 중 커밋된 후기·앵콜을 면제 재계산에서 놓칠 수 있다.
    private Flag lockFlag(Long flagId) {
        return flagRepository.findByIdForUpdate(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
    }

    private void refreshExemption(Flag flag) {
        exemptionUpdater.refresh(flag);
        flagRepository.save(flag);
    }

    private void validateBelongsToFlag(FlagMemorial memorial, Long flagId) {
        if (!flagId.equals(memorial.getFlagId())) {
            throw new FlagMemorialNotFoundException(memorial.getId());
        }
    }
}
