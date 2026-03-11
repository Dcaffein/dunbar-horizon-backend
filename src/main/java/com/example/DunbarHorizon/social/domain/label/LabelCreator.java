package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LabelCreator {

    private final LabelNamePolicy labelNamePolicy;

    public Label create(UserReference owner, String labelName, boolean exposure) {
        labelNamePolicy.validateLabelNameUniqueness(owner.getId(), labelName);
        return new Label(owner, labelName, exposure);
    }
}
