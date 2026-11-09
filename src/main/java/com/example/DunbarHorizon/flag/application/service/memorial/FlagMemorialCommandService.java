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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagMemorialCommandService implements FlagMemorialCommandUseCase {

    private final FlagRepository flagRepository;
    private final FlagMemorialRepository memorialRepository;
    private final FlagExpiryExemptionUpdater exemptionUpdater;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long createMemorial(Long flagId, Long userId, String content) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        boolean isParticipant = !flag.getHostId().equals(userId)
                && flagRepository.isParticipating(flagId, userId);
        FlagMemorial memorial = memorialRepository.save(
                FlagMemorial.create(flag, userId, content, isParticipant));
        refreshExemption(flagId);
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteMemorial(Long flagId, Long memorialId, Long requesterId) {
        FlagMemorial memorial = memorialRepository.findById(memorialId)
                .filter(found -> flagId.equals(found.getFlagId()))
                .orElseThrow(() -> new FlagMemorialNotFoundException(memorialId));
        memorial.validateDeletion(requesterId);
        memorialRepository.delete(memorial);
        refreshExemption(flagId);
    }

    // 원천 변경 뒤에 잠그므로 호출 트랜잭션은 READ COMMITTED여야 한다. 잠금을 기다리는 사이
    // 다른 트랜잭션이 커밋한 후기·앵콜을 면제 재계산이 봐야 한다.
    private void refreshExemption(Long flagId) {
        Flag flag = flagRepository.findLatestByIdForUpdate(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));
        exemptionUpdater.refresh(flag);
        flagRepository.save(flag);
    }

    private void validateBelongsToFlag(FlagMemorial memorial, Long flagId) {
        if (!flagId.equals(memorial.getFlagId())) {
            throw new FlagMemorialNotFoundException(memorial.getId());
        }
    }
}
