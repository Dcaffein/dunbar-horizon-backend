package com.example.DunbarHorizon.flag.application.service.memorial;

import com.example.DunbarHorizon.flag.application.port.in.FlagMemorialCommandUseCase;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorialManager;
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

    private final FlagMemorialManager memorialManager;
    private final FlagMemorialRepository memorialRepository;

    @Override
    public Long createMemorial(Long flagId, Long userId, String content) {
        return memorialManager.writeMemorial(flagId, userId, content).getId();
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
        memorialManager.deleteMemorial(flagId, memorialId, requesterId);
    }

    private void validateBelongsToFlag(FlagMemorial memorial, Long flagId) {
        if (!flagId.equals(memorial.getFlagId())) {
            throw new FlagMemorialNotFoundException(memorial.getId());
        }
    }
}
