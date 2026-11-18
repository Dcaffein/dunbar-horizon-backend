package com.example.DunbarHorizon.social.domain.label;


public class LabelTestFactory {

    public static Label createLabel(Long owner, String name) {
        return Label.create(owner, new LabelName(name));
    }
}
