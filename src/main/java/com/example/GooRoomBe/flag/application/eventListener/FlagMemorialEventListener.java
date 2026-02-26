package com.example.GooRoomBe.flag.application.eventListener;

import com.example.GooRoomBe.flag.domain.flag.FlagPreservationCriteria;
import com.example.GooRoomBe.flag.domain.memorial.event.MemorialDeletedEvent;
import com.example.GooRoomBe.flag.domain.memorial.event.MemorialCreatedEvent;
import com.example.GooRoomBe.flag.domain.flag.Flag;
import com.example.GooRoomBe.flag.domain.flag.exception.FlagNotFoundException;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagRepository;
import com.example.GooRoomBe.flag.domain.memorial.repository.FlagMemorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagMemorialEventListener {
    private final FlagRepository flagRepository;
    private final FlagMemorialRepository memorialRepository;

    @EventListener
    public void handleMemorialCreated(MemorialCreatedEvent event) {
        updateFlagPreservation(event.flagId());
    }

    @EventListener
    public void handleMemorialDeleted(MemorialDeletedEvent event) {
        updateFlagPreservation(event.flagId());
    }

    private void updateFlagPreservation(Long flagId) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));

        flag.updatePreservation(new FlagPreservationCriteria(
                memorialRepository.existsByFlagId(flagId),
                flagRepository.existsByParentId(flagId)
        ));
        flagRepository.save(flag);
    }
}