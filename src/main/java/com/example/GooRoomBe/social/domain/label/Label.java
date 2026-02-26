package com.example.GooRoomBe.social.domain.label;

import com.example.GooRoomBe.social.domain.socialUser.UserReference;
import com.example.GooRoomBe.social.domain.label.exception.InvalidLabelNameException;
import com.example.GooRoomBe.social.domain.label.exception.LabelAuthorizationException;
import lombok.*;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

import static com.example.GooRoomBe.social.domain.label.constant.LabelConstants.HAS_MEMBER;
import static com.example.GooRoomBe.social.domain.label.constant.LabelConstants.OWNS;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@Node("Label")
public class Label {
    @Id @GeneratedValue
    private String id;

    @Version
    private Long version;

    private String labelName;

    private boolean exposure;

    @Relationship(type = OWNS, direction = Relationship.Direction.INCOMING)
    private UserReference owner;

    @Relationship(type = HAS_MEMBER, direction = Relationship.Direction.OUTGOING)
    private Set<UserReference> members = new HashSet<>();

    Label(UserReference owner, String labelName, boolean exposure) {
        this.owner = owner;
        this.labelName = labelName;
        this.exposure = exposure;
    }

    void addNewMember(UserReference newMember) {
        this.members.add(newMember);
    }

    public void removeMember(UserReference memberToRemove) {
        members.remove(memberToRemove);
    }

    void updateMembers(Set<UserReference> newMembers) {
        if (newMembers == null || newMembers.isEmpty()) {
            this.members.clear();
            return;
        }
        this.members.removeIf(existing -> !newMembers.contains(existing));
        this.members.addAll(newMembers);
    }

    void applyNewLabelName(String newLabelName) {
        if (newLabelName == null || newLabelName.isBlank()) {
            throw new InvalidLabelNameException();
        }
        this.labelName = newLabelName;
    }

    public void updateExposure(Long currentUserId, boolean exposure) {
        validateOwner(currentUserId);
        this.exposure = exposure;
    }

    private void validateOwner(Long userId) {
        if (!this.owner.getId().equals(userId)) {
            throw new LabelAuthorizationException(userId);
        }
    }
}