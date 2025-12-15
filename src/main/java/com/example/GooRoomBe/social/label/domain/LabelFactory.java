package com.example.GooRoomBe.social.label.domain;

import com.example.GooRoomBe.social.socialUser.SocialUser;
import com.example.GooRoomBe.social.socialUser.SocialUserPort;
import com.example.GooRoomBe.social.label.domain.service.LabelNameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LabelFactory {

    private final SocialUserPort socialUserPort;
    private final LabelNameService labelNameService;

    public Label createLabel(String ownerId, String labelName, boolean exposure) {

        labelNameService.validateLabelNameUniqueness(ownerId, labelName);

        SocialUser owner = socialUserPort.getUser(ownerId);

        return new Label(owner, labelName, exposure);
    }
}