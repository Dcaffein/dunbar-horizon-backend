package com.example.DunbarHorizon.social.domain.label;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LabelFactory {

    private final LabelNamePolicy labelNamePolicy;

    public Label create(Long ownerId, String labelName) {
        labelNamePolicy.validateLabelNameUniqueness(ownerId, labelName);
        return new Label(ownerId, labelName);
    }
}
