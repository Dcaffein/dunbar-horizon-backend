package com.example.GooRoomBe.support;

import com.example.GooRoomBe.account.auth.security.SecurityConfig;
import com.example.GooRoomBe.account.auth.security.core.exception.CustomAuthenticationEntryPoint;
import com.example.GooRoomBe.account.auth.security.core.jwt.JwtAuthenticationFilter;
import com.example.GooRoomBe.account.auth.security.local.handler.LoginFailureHandler;
import com.example.GooRoomBe.account.auth.security.local.handler.LoginSuccessHandler;
import com.example.GooRoomBe.account.auth.security.oauth.OAuth2AuthenticationSuccessHandler;
import com.example.GooRoomBe.account.auth.security.local.CustomAuthenticationProvider;
import com.example.GooRoomBe.account.auth.security.oauth.CustomOAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;

@Import(SecurityConfig.class)
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean protected CustomAuthenticationProvider customAuthenticationProvider;
    @MockitoBean protected JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean protected LoginSuccessHandler loginSuccessHandler;
    @MockitoBean protected LoginFailureHandler loginFailureHandler;
    @MockitoBean protected OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean protected CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @MockitoBean protected CustomOAuth2UserService customOAuth2UserService;

    @BeforeEach
    void setUpFilter() throws Exception {
        willAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).given(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }
}