package com.example.GooRoomBe.social.domain.label;

import com.example.GooRoomBe.social.domain.socialUser.UserReference;
import java.util.Set;

public class LabelTestFactory {

    public static Label createLabel(UserReference owner, String name, boolean exposure) {
        return new Label(owner, name, exposure);
    }

    public static void addMember(Label label, UserReference member) {
        label.addNewMember(member);
    }

    public static void updateMembers(Label label, Set<UserReference> members) {
        label.updateMembers(members);
    }

    public static void changeName(Label label, String newName) {
        label.applyNewLabelName(newName);
    }
}