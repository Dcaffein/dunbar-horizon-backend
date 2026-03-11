package com.example.DunbarHorizon.support;

import com.example.DunbarHorizon.global.security.AuthPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        AuthPrincipal principal = new AuthPrincipal(Long.parseLong(annotation.id()), annotation.role());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, "password", List.of(new SimpleGrantedAuthority(annotation.role())));

        context.setAuthentication(auth);
        return context;
    }
}