package com.example.DunbarHorizon.account.application.service;

import com.example.DunbarHorizon.account.application.dto.UserProfileInfo;
import com.example.DunbarHorizon.account.domain.model.User;
import com.example.DunbarHorizon.account.domain.model.UserStatus;
import com.example.DunbarHorizon.account.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @InjectMocks private UserQueryService userQueryService;
    @Mock private UserRepository userRepository;

    @Test
    @DisplayName("ACTIVE 유저가 존재하면 UserProfileInfo를 반환한다")
    void findActiveUserByEmail_ActiveUser_ReturnsProfile() {
        // given
        User user = User.builder().email("test@test.com").nickname("tester").status(UserStatus.ACTIVE).build();
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        // when
        Optional<UserProfileInfo> result = userQueryService.findActiveUserByEmail("test@test.com");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().nickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("PENDING 유저는 이메일로 조회되지 않는다")
    void findActiveUserByEmail_PendingUser_ReturnsEmpty() {
        // given
        User user = User.builder().email("test@test.com").nickname("tester").status(UserStatus.PENDING).build();
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        // when
        Optional<UserProfileInfo> result = userQueryService.findActiveUserByEmail("test@test.com");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("등록되지 않은 이메일이면 빈 Optional을 반환한다")
    void findActiveUserByEmail_NoUser_ReturnsEmpty() {
        // given
        given(userRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

        // when
        Optional<UserProfileInfo> result = userQueryService.findActiveUserByEmail("notfound@test.com");

        // then
        assertThat(result).isEmpty();
    }
}
