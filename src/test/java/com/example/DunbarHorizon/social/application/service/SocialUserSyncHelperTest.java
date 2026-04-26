package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.social.application.port.out.UserProfilePort;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.UserReference;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class SocialUserSyncHelperTest {

    private SocialUserRepository socialUserRepository;
    private UserProfilePort userProfilePort;
    private SocialUserSyncHelper syncHelper;

    @BeforeEach
    void setUp() {
        socialUserRepository = mock(SocialUserRepository.class);
        userProfilePort = mock(UserProfilePort.class);
        syncHelper = new SocialUserSyncHelper(socialUserRepository, userProfilePort);
    }

    @Test
    void 미싱_유저_ID로_Account에서_프로필을_조회하여_Neo4j에_저장한다() {
        // given
        Set<Long> missingIds = Set.of(1L);
        UserProfileInfo profile = new UserProfileInfo(1L, "nick", "img.url");
        SocialUser saved = new SocialUser(1L, "nick", "img.url");

        given(userProfilePort.getUserProfiles(missingIds)).willReturn(List.of(profile));
        given(socialUserRepository.saveAll(anyList())).willReturn(Set.of(saved));

        // when
        Set<UserReference> result = syncHelper.syncAndSave(missingIds);

        // then
        assertThat(result).hasSize(1);
        verify(userProfilePort).getUserProfiles(missingIds);
        verify(socialUserRepository).saveAll(anyList());
    }

    @Test
    void Account에_유저가_없으면_빈_Set을_반환한다() {
        // given
        Set<Long> missingIds = Set.of(99L);
        given(userProfilePort.getUserProfiles(missingIds)).willReturn(List.of());
        given(socialUserRepository.saveAll(anyList())).willReturn(Set.of());

        // when
        Set<UserReference> result = syncHelper.syncAndSave(missingIds);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void DataIntegrityViolationException_발생_시_기존_노드를_조회하여_정상_반환한다() {
        // given
        Set<Long> missingIds = Set.of(1L);
        UserProfileInfo profile = new UserProfileInfo(1L, "nick", "img.url");
        SocialUser existing = new SocialUser(1L, "nick", "img.url");

        given(userProfilePort.getUserProfiles(missingIds)).willReturn(List.of(profile));
        given(socialUserRepository.saveAll(anyList())).willThrow(new DataIntegrityViolationException("duplicate"));
        given(socialUserRepository.findAllUserReferencesById(missingIds)).willReturn(Set.of(existing));

        // when
        Set<UserReference> result = syncHelper.syncAndSave(missingIds);

        // then
        assertThat(result).hasSize(1);
        verify(socialUserRepository).findAllUserReferencesById(missingIds);
    }
}
