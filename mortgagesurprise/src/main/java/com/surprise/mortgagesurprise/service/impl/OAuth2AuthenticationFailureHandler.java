package com.surprise.mortgagesurprise.service.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final String frontendCallbackUrl;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.frontend-callback-url}") String frontendCallbackUrl) {
        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 authentication failure: {}", exception.getMessage(), exception);

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
                .queryParam("error", exception.getLocalizedMessage() != null ? exception.getLocalizedMessage() : "Authentication failed")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
