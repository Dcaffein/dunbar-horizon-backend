package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.domain.friend.Friendship;

import java.util.Collection;

public interface ArchivedFriendshipRepository {
    void archive(Collection<Friendship> friendships);
}
