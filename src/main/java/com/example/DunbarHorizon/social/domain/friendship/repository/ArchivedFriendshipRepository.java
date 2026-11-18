package com.example.DunbarHorizon.social.domain.friendship.repository;

import com.example.DunbarHorizon.social.domain.friendship.Friendship;

import java.util.Collection;

public interface ArchivedFriendshipRepository {
    void archive(Collection<Friendship> friendships);
}
