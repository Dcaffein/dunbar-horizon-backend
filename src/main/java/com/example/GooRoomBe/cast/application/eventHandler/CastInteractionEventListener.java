package com.example.GooRoomBe.cast.application.eventHandler;

import com.example.GooRoomBe.cast.domain.event.CastCreatedEvent;
import com.example.GooRoomBe.cast.domain.event.CastRepliedEvent;
import com.example.GooRoomBe.global.event.interaction.InteractionType;
import com.example.GooRoomBe.global.event.interaction.UserInteractionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CastInteractionEventListener {

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onCastCreated(CastCreatedEvent event) {
        event.recipientIds().forEach(recipientId ->
                eventPublisher.publishEvent(new UserInteractionEvent(
                        event.creatorId(),
                        recipientId,
                        InteractionType.CAST_SEND
                ))
        );
    }

    @EventListener
    public void onCastReplied(CastRepliedEvent event) {
        eventPublisher.publishEvent(new UserInteractionEvent(
                event.replierId(),
                event.creatorId(),
                InteractionType.CAST_REPLY
        ));
    }
}
