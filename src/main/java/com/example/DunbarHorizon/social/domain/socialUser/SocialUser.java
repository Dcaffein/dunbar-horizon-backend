package com.example.DunbarHorizon.social.domain.socialUser;

import com.example.DunbarHorizon.social.domain.socialUser.constant.SocialUserConstants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.DynamicLabels;

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

    private void activate() {
        this.statusLabels.clear();
        this.statusLabels.add(SocialUserConstants.USER_REFERENCE);
    }

    private void deactivate() {
        this.statusLabels.clear();
        this.statusLabels.add(SocialUserConstants.INACTIVE_SOCIAL_USER);
    }
}