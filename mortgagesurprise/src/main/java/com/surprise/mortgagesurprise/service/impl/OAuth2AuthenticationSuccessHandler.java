package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthService authService;
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

        AuthResponse authResponse = authService.createAuthResponse(user);

        String code = codeStore.save(authResponse);

        String targetCallbackUrl = frontendCallbackUrl;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("oauth2_redirect_uri".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    String decoded = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
                    if (decoded.startsWith("http://localhost:") || decoded.startsWith("http://localhost/") || decoded.startsWith("http://127.0.0.1:")) {
                        targetCallbackUrl = decoded;
                    }
                    break;
                }
            }
        }

        Cookie cleanupCookie = new Cookie("oauth2_redirect_uri", "");
        cleanupCookie.setPath("/");
        cleanupCookie.setMaxAge(0);
        response.addCookie(cleanupCookie);

        String redirectUrl = UriComponentsBuilder.fromUriString(targetCallbackUrl)
                .queryParam("code", code)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}