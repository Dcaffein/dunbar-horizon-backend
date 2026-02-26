package com.example.GooRoomBe.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // 컨트롤러의 expression = "id"가 읽을 수 있도록 id 필드를 가진 Principal 생성
        // 프로젝트의 실제 UserPrincipal 클래스가 있다면 그것을 사용해도 좋습니다.
        Map<String, Object> attributes = Map.of("id", Long.parseLong(annotation.id()));

        // 헬퍼 클래스 혹은 가짜 객체를 만들어 Principal로 설정
        CustomUserDetails principal = new CustomUserDetails(Long.parseLong(annotation.id()), "test_user");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, "password", List.of(new SimpleGrantedAuthority(annotation.role())));

        context.setAuthentication(auth);
        return context;
    }

    // 간단한 테스트용 내부 클래스 (id 필드가 반드시 있어야 함)
    @Getter
    @AllArgsConstructor
    public static class CustomUserDetails {
        private Long id;
        private String username;
    }
}