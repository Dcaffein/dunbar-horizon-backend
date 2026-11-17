package com.example.DunbarHorizon.social.application.port.out;

import com.example.DunbarHorizon.social.application.dto.FriendView;

import java.util.List;
import java.util.Optional;

public interface FriendViewRepository {
    List<FriendView> findFriends(Long userId);

    Optional<FriendView> findFriend(Long userId, Long friendId);
}
