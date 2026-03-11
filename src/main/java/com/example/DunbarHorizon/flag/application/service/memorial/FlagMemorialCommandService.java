package com.example.DunbarHorizon.flag.application.service.memorial;

import com.example.DunbarHorizon.flag.application.port.in.FlagMemorialCommandUseCase;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagNotFoundException;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagRepository;
import com.example.DunbarHorizon.flag.domain.memorial.DeletableFlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorialCreator;
import com.example.DunbarHorizon.flag.domain.memorial.event.MemorialCreatedEvent;
import com.example.DunbarHorizon.flag.domain.memorial.event.MemorialDeletedEvent;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialNotFoundException;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagMemorialCommandService implements FlagMemorialCommandUseCase {

    private final FlagRepository flagRepository;
    private final FlagMemorialRepository memorialRepository;
    private final FlagMemorialCreator memorialCreator;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Long createMemorial(Long flagId, Long userId, String content) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));

        FlagMemorial newMemorial = memorialCreator.create(flag, userId, content);

        FlagMemorial saved = memorialRepository.save(newMemorial);
        eventPublisher.publishEvent(new MemorialCreatedEvent(flagId));

        return saved.getId();
    }

    @Override
    public void updateMemorial(Long memorialId, Long requesterId, String content) {
        FlagMemorial memorial = memorialRepository.findById(memorialId)
                .orElseThrow(() -> new FlagMemorialNotFoundException(memorialId));

        memorial.updateContent(requesterId, content);
    }

    @Override
    public void deleteMemorial(Long memorialId, Long requesterId) {
        FlagMemorial memorial = memorialRepository.findById(memorialId)
                .orElseThrow(() -> new FlagMemorialNotFoundException(memorialId));

        DeletableFlagMemorial ticket = memorial.verifyForDeletion(requesterId);

        memorialRepository.delete(ticket);
        eventPublisher.publishEvent(new MemorialDeletedEvent(memorial.getFlagId()));
    }
}