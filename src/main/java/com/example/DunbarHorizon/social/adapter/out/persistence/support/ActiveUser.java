package com.example.DunbarHorizon.social.adapter.out.persistence.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Immutable
@SQLRestriction("status = 'ACTIVE'")
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActiveUser {

    @Id
    @Column(name = "user_id")
    private Long id;
}
