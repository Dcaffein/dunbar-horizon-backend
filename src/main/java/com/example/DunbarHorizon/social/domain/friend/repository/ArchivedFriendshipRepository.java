package com.example.DunbarHorizon.social.domain.friend.repository;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ArchivedFriendship;

import java.util.List;

public interface ArchivedFriendshipRepository {
    void saveAll(List<ArchivedFriendship> friendships);
}
