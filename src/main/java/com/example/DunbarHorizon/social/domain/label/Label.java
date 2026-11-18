package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.global.util.UuidUtil;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
import com.example.DunbarHorizon.social.domain.label.exception.InvalidLabelNameException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


@Entity
@DynamicUpdate
@Table(name = "labels")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public class Label {

    public static final int NAME_MAX_LENGTH = 20;
    public static final String NAME_LENGTH_MESSAGE = "라벨 이름은 1자 이상 20자 이하로 입력해주세요.";

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String labelName;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @ElementCollection
    @CollectionTable(name = "label_members",
            joinColumns = @JoinColumn(name = "label_id", nullable = false),
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Column(name = "member_id", nullable = false)
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

    void addMember(Long memberId) {
        if (!memberIds.add(memberId)) {
            throw new DuplicateLabelMemberException(memberId);
        }
    }

    void replaceMembers(Collection<Long> requestedIds) {
        Set<Long> replacement = new HashSet<>(requestedIds);
        memberIds.clear();
        memberIds.addAll(replacement);
    }

    public void removeMember(Long memberId) {
        memberIds.remove(memberId);
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > NAME_MAX_LENGTH) {
            throw new InvalidLabelNameException();
        }
    }

}
