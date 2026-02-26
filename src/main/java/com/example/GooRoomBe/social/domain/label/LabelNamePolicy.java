package com.example.GooRoomBe.social.domain.label;

import com.example.GooRoomBe.social.domain.label.exception.DuplicateLabelNameException;
import com.example.GooRoomBe.social.domain.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LabelNamePolicy {
    private final LabelRepository labelRepository;

    public void changeLabelName(Label targetLabel, String newLabelName) {
        if (targetLabel.getLabelName().equals(newLabelName)) {
            return;
        }

        Long ownerId = targetLabel.getOwner().getId();
        validateLabelNameUniqueness(ownerId, newLabelName);
        targetLabel.applyNewLabelName(newLabelName);
    }

    public void validateLabelNameUniqueness(Long ownerId, String labelName) {
        if (labelRepository.existsByOwner_IdAndLabelName(ownerId, labelName)) {
            throw new DuplicateLabelNameException(ownerId, labelName);
        }
    }
}