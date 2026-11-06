MATCH (u:SocialUser)-[recognition:HAS_FRIENDSHIP]->(f:Friendship)
WITH f, u, recognition ORDER BY u.id
WITH f, collect(u) AS users, collect(recognition) AS recognitions
WHERE size(users) = 2
WITH f, users[0] AS low, users[1] AS high, recognitions[0] AS lowRecognition, recognitions[1] AS highRecognition
MERGE (low)-[r:FRIENDS_WITH {id: f.id}]->(high)
ON CREATE SET r.createdAt = f.createdAt,
              r.intimacy = f.intimacy,
              r.lowFriendAlias = lowRecognition.friendAlias,
              r.lowInterestScore = lowRecognition.interestScore,
              r.lowLastInteractedAt = lowRecognition.lastInteractedAt,
              r.lowIsMuted = coalesce(lowRecognition.isMuted, false),
              r.lowIsRoutable = coalesce(lowRecognition.isRoutable, true),
              r.highFriendAlias = highRecognition.friendAlias,
              r.highInterestScore = highRecognition.interestScore,
              r.highLastInteractedAt = highRecognition.lastInteractedAt,
              r.highIsMuted = coalesce(highRecognition.isMuted, false),
              r.highIsRoutable = coalesce(highRecognition.isRoutable, true);
