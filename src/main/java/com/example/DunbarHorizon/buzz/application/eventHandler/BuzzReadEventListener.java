package com.example.DunbarHorizon.buzz.application.eventHandler;


import com.example.DunbarHorizon.buzz.adapter.out.persistence.mongo.BuzzMongoTemplateRepository;
import com.example.DunbarHorizon.buzz.domain.event.BuzzReadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BuzzReadEventListener {

    private final BuzzMongoTemplateRepository buzzRepository;

    @Async
    @EventListener
    public void handleBuzzRead(BuzzReadEvent event) {
        buzzRepository.addReadRecipient(event.buzzId(), event.userId());
    }
}