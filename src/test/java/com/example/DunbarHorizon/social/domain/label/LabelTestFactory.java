package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;

public class LabelTestFactory {

    public static Label createLabel(UserReference owner, String name) {
        return new Label(owner, name);
    }

    public static void changeName(Label label, String newName) {
        label.applyNewLabelName(newName);
    }
}