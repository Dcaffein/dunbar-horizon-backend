package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelNameException;
import org.springframework.stereotype.Component;

@Component
public class LabelNamer {

    public Label create(Long ownerId, String name, boolean nameExists) {
        requireUniqueName(ownerId, name, nameExists);
        return new Label(ownerId, name);
    }

    public void rename(Label label, String name, boolean nameUsedByOtherLabel) {
        requireUniqueName(label.getOwnerId(), name, nameUsedByOtherLabel);
        label.applyNewLabelName(name);
    }

    private void requireUniqueName(Long ownerId, String name, boolean nameExists) {
        if (nameExists) {
            throw new DuplicateLabelNameException(ownerId, name);
        }
    }
}
