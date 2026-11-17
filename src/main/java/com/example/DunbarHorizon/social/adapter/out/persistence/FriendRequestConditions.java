package com.example.DunbarHorizon.social.adapter.out.persistence;

public final class FriendRequestConditions {

    public static final String ACTIVE_PAIR = """
            EXISTS (SELECT 1 FROM users WHERE user_id = requester_id AND status = 'ACTIVE')
              AND EXISTS (SELECT 1 FROM users WHERE user_id = receiver_id AND status = 'ACTIVE')""";

    private FriendRequestConditions() {
    }
}
