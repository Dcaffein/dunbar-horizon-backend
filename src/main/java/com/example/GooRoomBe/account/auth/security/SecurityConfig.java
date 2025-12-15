package com.example.GooRoomBe.account.auth.security;

import com.example.GooRoomBe.account.auth.security.oauth.OAuth2AuthenticationSuccessHandler;
import com.example.GooRoomBe.account.auth.security.local.CustomAuthenticationProvider;
import com.example.GooRoomBe.account.auth.security.local.JsonUsernamePasswordAuthenticationFilter;
import com.example.GooRoomBe.account.auth.security.local.handler.LoginFailureHandler;
import com.example.GooRoomBe.account.auth.security.local.handler.LoginSuccessHandler;
import com.example.GooRoomBe.account.auth.security.core.jwt.JwtAuthenticationFilter;
import com.example.GooRoomBe.account.auth.security.oauth.CustomOAuth2UserService;
import com.example.GooRoomBe.account.auth.security.core.exception.CustomAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
            AuthenticationManager authenticationManager,
            LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler,
            ObjectMapper objectMapper
    ) {
        JsonUsernamePasswordAuthenticationFilter jsonAuthFilter = new JsonUsernamePasswordAuthenticationFilter(objectMapper);
        jsonAuthFilter.setFilterProcessesUrl("/api/v1/auth/login");
        jsonAuthFilter.setAuthenticationManager(authenticationManager);
        jsonAuthFilter.setAuthenticationSuccessHandler(loginSuccessHandler);
        jsonAuthFilter.setAuthenticationFailureHandler(loginFailureHandler);
        return jsonAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationManager authenticationManager,
                                           JsonUsernamePasswordAuthenticationFilter jsonAuthFilter,
                                           CustomAuthenticationProvider customAuthenticationProvider,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           CustomOAuth2UserService customOAuth2UserService,
                                           OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                                           CustomAuthenticationEntryPoint customAuthenticationEntryPoint
    ) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/email-verifications",
                                "/api/v1/users"
                        )
                        .ignoringRequestMatchers(request ->
                                "POST".equals(request.getMethod()) &&
                                        "/api/v1/auth/tokens".equals(request.getRequestURI())
                        )
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http
                .authenticationProvider(customAuthenticationProvider)
                .addFilterAt(jsonAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, JsonUsernamePasswordAuthenticationFilter.class);

        // OAuth2 로그인 설정
        http
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler));

        // URL 권한 설정
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/email-verifications",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/tokens").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .anyRequest().authenticated());

        // 예외 처리 설정
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
        );

        return http.build();
    }
}