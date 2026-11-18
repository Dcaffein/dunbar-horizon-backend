package com.example.DunbarHorizon.social.adapter.out.persistence.jdbc;

final class FriendshipConditions {

    static final String ACTIVE_PAIR = """
            EXISTS (SELECT 1 FROM users WHERE user_id = low_user_id AND status = 'ACTIVE')
              AND EXISTS (SELECT 1 FROM users WHERE user_id = high_user_id AND status = 'ACTIVE')""";

    private FriendshipConditions() {
    }
}
