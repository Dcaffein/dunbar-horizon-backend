package com.example.GooRoomBe.cast.application.command;

import com.example.GooRoomBe.cast.application.command.recipient.RecipientPayload;

import java.util.List;

public record CreateCastCommand(
        Long creatorId,
        String text,
        List<String> imageUrls,
        RecipientPayload payload
) {}