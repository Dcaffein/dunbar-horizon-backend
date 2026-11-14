package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;

public interface FriendshipLifecycleRepository extends FriendshipRepository {
    void updateUserFields(Long userId, Long friendId, String alias, Boolean muted, Boolean routable);
}
