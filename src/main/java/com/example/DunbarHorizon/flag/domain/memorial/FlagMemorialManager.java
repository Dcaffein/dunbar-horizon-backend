package com.example.DunbarHorizon.flag.domain.memorial;

import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.FlagExpiryExemptionUpdater;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialNotFoundException;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 각 메서드는 트랜잭션의 첫 호출이어야 한다. REPEATABLE READ는 첫 일반 조회 시점에 읽기 뷰를 고정하므로,
 * 호출 전에 일반 조회가 끼면 잠금을 기다리는 동안 커밋된 후기가 면제 판단에서 빠진다.
 */
@Component
@RequiredArgsConstructor
public class FlagMemorialManager {

    private final FlagRepository flagRepository;
    private final FlagMemorialRepository memorialRepository;
    private final FlagMemorialFactory memorialFactory;
    private final FlagExpiryExemptionUpdater exemptionUpdater;

    public FlagMemorial writeMemorial(Long flagId, Long writerId, String content) {
        Flag flag = lock(flagId);
        FlagMemorial memorial = memorialRepository.save(memorialFactory.create(flag, writerId, content));
        refreshExemption(flag);
        return memorial;
    }

    public void deleteMemorial(Long flagId, Long memorialId, Long requesterId) {
        Flag flag = lock(flagId);
        FlagMemorial memorial = memorialRepository.findById(memorialId)
                .filter(found -> flagId.equals(found.getFlagId()))
                .orElseThrow(() -> new FlagMemorialNotFoundException(memorialId));
        memorial.validateDeletion(requesterId);
        memorialRepository.delete(memorial);
        refreshExemption(flag);
    }

    private Flag lock(Long flagId) {
        return flagRepository.findByIdForUpdate(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
    }

    private void refreshExemption(Flag flag) {
        exemptionUpdater.refresh(flag);
        flagRepository.save(flag);
    }
}
