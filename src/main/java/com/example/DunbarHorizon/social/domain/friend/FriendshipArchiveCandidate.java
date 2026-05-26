package com.example.DunbarHorizon.social.domain.friend;

import java.time.LocalDate;

public record FriendshipArchiveCandidate(String id, Long userAId, Long userBId, LocalDate friendedAt) {}
