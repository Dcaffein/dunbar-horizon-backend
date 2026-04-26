package com.example.DunbarHorizon.social.domain.socialUser;

import com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.DynamicLabels;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Node("SocialUser")
public class SocialUser implements UserReference {
    @Id
    private Long id;

    private String nickname;
    private String profileImageUrl;

    private LocalDateTime updatedAt;

    @DynamicLabels
    private Set<String> statusLabels = new HashSet<>();

    public SocialUser(Long id, String nickname, String profileImageUrl) {
        this.id = id;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        activate();
    }

    public void switchUserStatus(boolean active) {
        if (active) {
            activate();
        } else {
            deactivate();
        }
    }

    /**
     * Last-Write-Wins: occurredAt이 현재 updatedAt보다 최신일 때만 프로필을 갱신한다.
     * updatedAt이 null이면 최초 갱신으로 간주하여 항상 적용한다.
     */
    public void updateProfile(String nickname, String profileImageUrl, LocalDateTime occurredAt) {
        if (!isNewerThan(occurredAt)) return;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = occurredAt;
    }

    public boolean isNewerThan(LocalDateTime occurredAt) {
        return this.updatedAt == null || occurredAt.isAfter(this.updatedAt);
    }

    private void activate() {
        this.statusLabels.clear();
        this.statusLabels.add(SocialUserConstants.USER_REFERENCE);
    }

    private void deactivate() {
        this.statusLabels.clear();
        this.statusLabels.add(SocialUserConstants.INACTIVE_SOCIAL_USER);
    }
}
