package com.example.DunbarHorizon.buzz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;

@Entity
@Table(name = "buzz_audience_members")
@IdClass(BuzzAudienceMember.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuzzAudienceMember implements Persistable<BuzzAudienceMember.Key> {

    @Id
    @Column(length = 36)
    private String buzzId;

    @Id
    private Long userId;

    @Column(nullable = false)
    private boolean isRead;

    @Transient
    private boolean isNew;

    private BuzzAudienceMember(String buzzId, Long userId) {
        this.buzzId = buzzId;
        this.userId = userId;
        this.isRead = false;
        this.isNew = true;
    }

    public static BuzzAudienceMember create(String buzzId, Long userId) {
        return new BuzzAudienceMember(buzzId, userId);
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public boolean isUnread() {
        return !isRead;
    }

    @Override
    public Key getId() {
        return new Key(buzzId, userId);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String buzzId;
        private Long userId;
    }
}
