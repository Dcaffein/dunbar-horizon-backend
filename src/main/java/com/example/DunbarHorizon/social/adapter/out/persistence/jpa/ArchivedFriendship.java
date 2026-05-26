package com.example.DunbarHorizon.social.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "archived_friendships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchivedFriendship {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private Long userAId;

    @Column(nullable = false)
    private Long userBId;

    private LocalDate friendedAt;

    @Column(nullable = false)
    private LocalDateTime archivedAt;

    public ArchivedFriendship(String id, Long userAId, Long userBId, LocalDate friendedAt) {
        this.id = id;
        this.userAId = userAId;
        this.userBId = userBId;
        this.friendedAt = friendedAt;
        this.archivedAt = LocalDateTime.now();
    }
}
