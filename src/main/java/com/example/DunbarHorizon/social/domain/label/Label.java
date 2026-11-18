package com.example.DunbarHorizon.social.domain.label;

import com.example.DunbarHorizon.global.util.UuidUtil;
import com.example.DunbarHorizon.social.domain.label.exception.DuplicateLabelMemberException;
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
@Getter
public class Label {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = LabelName.MAX_LENGTH)
    private String labelName;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @ElementCollection
    @CollectionTable(name = "label_members",
            joinColumns = @JoinColumn(name = "label_id", nullable = false),
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Column(name = "member_id", nullable = false)
    private Set<Long> memberIds = new HashSet<>();

    private Label(Long ownerId, LabelName name) {
        this.id = UuidUtil.createV7().toString();
        this.ownerId = ownerId;
        this.labelName = name.value();
    }

    public static Label create(Long ownerId, LabelName name) {
        return new Label(ownerId, name);
    }

    public Set<Long> getMemberIds() {
        return Set.copyOf(memberIds);
    }

    public void rename(LabelName name) {
        this.labelName = name.value();
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
}
