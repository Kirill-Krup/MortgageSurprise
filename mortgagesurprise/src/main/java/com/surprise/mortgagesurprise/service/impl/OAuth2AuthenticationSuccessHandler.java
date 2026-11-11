package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthService authService;         // ваш существующий сервис, генерирующий AuthResponse
    private final OAuth2CodeStore codeStore;
    private final String frontendCallbackUrl;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            AuthService authService,
            OAuth2CodeStore codeStore,
            @Value("${app.oauth2.frontend-callback-url}") String frontendCallbackUrl) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.codeStore = codeStore;
        this.frontendCallbackUrl = frontendCallbackUrl;
        // Не редиректим на дефолтный "/", а переопределяем
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        UUID userId = UUID.fromString(principal.getAttribute("userId"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found after OAuth2 login"));

        // Генерируем access + refresh (используем ваш существующий сервис)
        AuthResponse authResponse = authService.createAuthResponse(user);

        // Сохраняем одноразовый код
        String code = codeStore.save(authResponse);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("code", code)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}