package com.example.DunbarHorizon.buzz.application.dto.command.recipient;

import java.util.List;

public record ManualRecipientSpec(List<Long> memberIds) implements RecipientSpec {
}
