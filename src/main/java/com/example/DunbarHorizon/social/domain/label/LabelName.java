package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.label.exception.InvalidLabelNameException;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class LabelName {

    public static final int MAX_LENGTH = 20;
    public static final String LENGTH_MESSAGE = "라벨 이름은 1자 이상 20자 이하로 입력해주세요.";

    private final String value;

    LabelName(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new InvalidLabelNameException();
        }
        this.value = value;
    }

    public String value() {
        return value;
    }
}
