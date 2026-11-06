package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.schema;

public final class SocialGraphSchema {
    public static final String PROP_ID = "id";
    public static final String PROP_NICKNAME = "nickname";
    public static final String PROP_PROFILE_IMAGE_URL = "profileImageUrl";
    public static final String PROP_CREATED_AT = "createdAt";
    public static final String PROP_INTIMACY = "intimacy";
    public static final String PROP_EXPOSURE = "exposure";

    public static final String SIDE_LOW = "low";
    public static final String SIDE_HIGH = "high";
    public static final String SUFFIX_FRIEND_ALIAS = "FriendAlias";
    public static final String SUFFIX_INTEREST_SCORE = "InterestScore";
    public static final String SUFFIX_LAST_INTERACTED_AT = "LastInteractedAt";
    public static final String SUFFIX_IS_MUTED = "IsMuted";
    public static final String SUFFIX_IS_ROUTABLE = "IsRoutable";

    private SocialGraphSchema() {}
}
