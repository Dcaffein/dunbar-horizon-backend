package com.example.DunbarHorizon.account.domain.model;

import com.example.DunbarHorizon.global.event.user.UserActivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserDeactivatedEvent;
import com.example.DunbarHorizon.global.event.user.UserProfileUpdatedEvent;
import com.example.DunbarHorizon.account.domain.event.UserDeletedEvent;
import java.time.LocalDateTime;
import com.example.DunbarHorizon.global.common.BaseTimeAggregateRoot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeAggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 20)
    private String nickname;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Builder
    public User(String email, String nickname, String profileImage, UserRole role, UserStatus status) {
        this.email = email;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.role = role != null ? role : UserRole.USER;
        this.status = status != null ? status : UserStatus.PENDING;
    }

    public static User createActiveOAuthUser(String email, String nickname) {
        return User.builder()
                .email(email)
                .nickname(nickname)
                .status(UserStatus.ACTIVE)
                .build();
    }


    public boolean isPending() {
        return this.status == UserStatus.PENDING;
    }

    public void updateProfile(String nickname, String profileImage) {
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.registerEvent(new UserProfileUpdatedEvent(this.id, nickname, profileImage, LocalDateTime.now()));
    }

    public void overwritePendingProfile(String nickname) {
        if (!this.isPending()) {
            throw new IllegalStateException("정식 회원의 프로필은 덮어쓸 수 없습니다.");
        }
        this.nickname = nickname;
        this.profileImage = null;
    }

    public void deactivate() {
        if (this.status == UserStatus.ACTIVE) {
            this.status = UserStatus.DORMANT;
            this.registerEvent(new UserDeactivatedEvent(this.id));
        }
    }

    public void activate() {
        if (this.status != UserStatus.ACTIVE) {
            this.status = UserStatus.ACTIVE;
            this.registerEvent(new UserActivatedEvent(this.id, this.nickname, this.profileImage));
        }
    }

    public void deleteAccount() {
        if (this.status != UserStatus.DELETED) {
            this.status = UserStatus.DELETED;
            this.registerEvent(new UserDeletedEvent(this.id));
        }
    }
}