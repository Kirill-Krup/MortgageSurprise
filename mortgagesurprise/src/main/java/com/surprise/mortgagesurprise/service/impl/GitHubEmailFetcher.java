package com.surprise.mortgagesurprise.service.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class GitHubEmailFetcher {

    private final RestClient restClient = RestClient.builder()
            .defaultHeader("User-Agent", "MortgageSurprise-App/1.0")
            .build();

    public String fetchPrimaryVerifiedEmail(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "MortgageSurprise-App/1.0")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (emails == null) {
                return null;
            }
            return emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("primary"))
                            && Boolean.TRUE.equals(email.get("verified")))
                    .map(email -> email.get("email"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .findFirst()
                    .orElse(null);
        } catch (RestClientException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("github_email_lookup_failed"),
                    "Unable to retrieve a verified GitHub email",
                    exception);
        }
    }
}
