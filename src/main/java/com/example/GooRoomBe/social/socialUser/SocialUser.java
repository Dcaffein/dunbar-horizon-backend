package com.example.GooRoomBe.social.socialUser;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import static com.example.GooRoomBe.social.common.SocialSchemaConstants.SOCIAL_USER;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Node(SOCIAL_USER)
public class SocialUser {
    @Id
    private String id;

    @ReadOnlyProperty private String nickname;
}