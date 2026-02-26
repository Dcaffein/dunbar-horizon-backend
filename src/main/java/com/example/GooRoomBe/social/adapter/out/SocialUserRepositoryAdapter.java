package com.example.GooRoomBe.social.adapter.out;

import com.example.GooRoomBe.social.adapter.out.neo4j.springData.SocialUserNeo4jRepository;
import com.example.GooRoomBe.social.domain.socialUser.SocialUser;
import com.example.GooRoomBe.social.domain.socialUser.SocialUserRepository;
import com.example.GooRoomBe.social.domain.socialUser.UserReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class SocialUserRepositoryAdapter implements SocialUserRepository {

    private final SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Override
    public Optional<SocialUser> findById(Long id) {
        return socialUserNeo4jRepository.findById(id);
    }

    @Override
    public Set<UserReference> findAllUserReferencesById(Collection<Long> ids) {
        return socialUserNeo4jRepository.findAllUserReferencesById(ids);
    }

    @Override
    public SocialUser save(SocialUser socialUser) {
        return socialUserNeo4jRepository.save(socialUser);
    }

    @Override
    public Set<SocialUser> saveAll(List<SocialUser> newUsers) {
        return new HashSet<>(socialUserNeo4jRepository.saveAll(newUsers));
    }
}
