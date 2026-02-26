package com.example.GooRoomBe.cast.application.service.recipient;

import com.example.GooRoomBe.cast.application.command.recipient.RecipientPayload;

import java.util.Set;

public interface RecipientStrategy {
    RecipientType getSupportType();
    Set<Long> fetchRecipientIds(Long creatorId, RecipientPayload payload);
}
