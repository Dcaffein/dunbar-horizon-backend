package com.example.DunbarHorizon.buzz.application.dto.command.recipient;

public sealed interface RecipientSpec permits ManualRecipientSpec, LabelRecipientSpec, IntermediaryRecipientSpec {
}
