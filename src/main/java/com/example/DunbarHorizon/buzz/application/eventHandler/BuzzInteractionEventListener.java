package com.example.DunbarHorizon.buzz.application.eventHandler;

import com.example.DunbarHorizon.buzz.domain.event.BuzzCommentedEvent;
import com.example.DunbarHorizon.buzz.domain.event.BuzzCreatedEvent;
import com.example.DunbarHorizon.global.event.interaction.InteractionType;
import com.example.DunbarHorizon.global.event.interaction.UserInteractionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BuzzInteractionEventListener {

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onBuzzCreated(BuzzCreatedEvent event) {
        event.recipientIds().forEach(recipientId ->
                eventPublisher.publishEvent(new UserInteractionEvent(
                        event.creatorId(),
                        recipientId,
                        InteractionType.BUZZ_SEND
                ))
        );
    }

    @EventListener
    public void onBuzzCommented(BuzzCommentedEvent event) {
        eventPublisher.publishEvent(new UserInteractionEvent(
                event.commenterId(),
                event.creatorId(),
                InteractionType.BUZZ_REPLY
        ));
    }
}
