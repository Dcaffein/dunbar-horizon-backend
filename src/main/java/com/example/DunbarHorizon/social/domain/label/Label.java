package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.label.exception.InvalidLabelNameException;
import com.example.DunbarHorizon.social.domain.label.exception.LabelAuthorizationException;
import lombok.*;
import org.springframework.data.annotation.Version;
import com.example.DunbarHorizon.global.util.UuidUtil;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.ATTACHED_TO;
import static com.example.DunbarHorizon.social.domain.label.constant.LabelConstants.OWNS_LABEL;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@Node("Label")
public class Label {
    @Id
    private String id;

    @Version
    private Long version;

    private String labelName;

    private boolean exposure;

    @Relationship(type = OWNS_LABEL, direction = Relationship.Direction.INCOMING)
    private UserReference owner;

    @Relationship(type = ATTACHED_TO, direction = Relationship.Direction.OUTGOING)
    private Set<UserReference> members = new HashSet<>();

    Label(UserReference owner, String labelName, boolean exposure) {
        this.id = UuidUtil.createV7().toString();
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