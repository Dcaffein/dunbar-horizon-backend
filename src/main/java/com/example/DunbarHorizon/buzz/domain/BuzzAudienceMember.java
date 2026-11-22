package com.example.DunbarHorizon.buzz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "buzz_audience_members")
@IdClass(BuzzAudienceMember.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuzzAudienceMember {

    @Id
    private Long buzzId;

    @Id
    private Long userId;

    @Column(nullable = false)
    private boolean isRead;

    private BuzzAudienceMember(Long buzzId, Long userId) {
        this.buzzId = buzzId;
        this.userId = userId;
        this.isRead = false;
    }

    public static BuzzAudienceMember create(Long buzzId, Long userId) {
        return new BuzzAudienceMember(buzzId, userId);
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public boolean isUnread() {
        return !isRead;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long buzzId;
        private Long userId;
    }
}
