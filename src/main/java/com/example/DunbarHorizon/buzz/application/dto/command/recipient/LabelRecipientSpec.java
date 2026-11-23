package com.example.DunbarHorizon.buzz.application.dto.command.recipient;

import java.util.List;

public record LabelRecipientSpec(List<String> labelIds) implements RecipientSpec {
}
