package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.label.exception.InvalidLabelNameException;
import lombok.*;
import com.example.DunbarHorizon.global.util.UuidUtil;

import java.util.HashSet;
import java.util.Set;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public class Label {

    public static final int NAME_MAX_LENGTH = 20;
    public static final String NAME_LENGTH_MESSAGE = "라벨 이름은 1자 이상 20자 이하로 입력해주세요.";

    private String id;

    private String labelName;

    private Long ownerId;

    private Set<Long> memberIds = new HashSet<>();

    Label(Long ownerId, String labelName) {
        validateName(labelName);
        this.id = UuidUtil.createV7().toString();
        this.ownerId = ownerId;
        this.labelName = labelName;
    }

    public static Label restore(String id, String labelName, Long ownerId, Set<Long> memberIds) {
        return new Label(id, labelName, ownerId, new HashSet<>(memberIds));
    }

    public Set<Long> getMemberIds() {
        return Set.copyOf(memberIds);
    }

    void applyNewLabelName(String newLabelName) {
        validateName(newLabelName);
        this.labelName = newLabelName;
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > NAME_MAX_LENGTH) {
            throw new InvalidLabelNameException();
        }
    }

}
