package com.example.DunbarHorizon.social.domain.label;


public class LabelTestFactory {

    public static Label createLabel(Long owner, String name) {
        return new Label(owner, name);
    }

    public static void changeName(Label label, String newName) {
        label.applyNewLabelName(newName);
    }
}
