package com.example.GooRoomBe.account.application.port.in.dto;

import com.example.GooRoomBe.account.domain.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public record AuthPrincipal(Long id, String role) {
    public static AuthPrincipal from(User user) {
        return new AuthPrincipal(user.getId(), user.getRole().getKey());
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
}