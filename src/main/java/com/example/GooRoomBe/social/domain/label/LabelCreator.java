package com.example.GooRoomBe.social.domain.label;

import com.example.GooRoomBe.social.domain.socialUser.UserReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Label 생성 정책.
 *
 * Label 생성자는 package-private으로 캡슐화되어 있으므로,
 * 반드시 이 클래스를 통해 생성해야 한다.
 * LabelNamePolicy 우회를 구조적으로 방지한다.
 */
@Component
@RequiredArgsConstructor
public class LabelCreator {

    private final LabelNamePolicy labelNamePolicy;

    public Label create(UserReference owner, String labelName, boolean exposure) {
        labelNamePolicy.validateLabelNameUniqueness(owner.getId(), labelName);
        return new Label(owner, labelName, exposure);
    }
}
