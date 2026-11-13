package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.model.entity.Role;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.security.oauth2.OAuth2UserInfo;
import com.surprise.mortgagesurprise.security.oauth2.OAuth2UserInfoFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;
    private final GitHubEmailFetcher gitHubEmailFetcher;

    public CustomOAuth2UserService(UserRepository userRepository, GitHubEmailFetcher gitHubEmailFetcher) {
        this.userRepository = userRepository;
        this.gitHubEmailFetcher = gitHubEmailFetcher;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User providerUser = delegate.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId().toLowerCase(Locale.ROOT);
        OAuth2UserInfo info = OAuth2UserInfoFactory.create(provider, providerUser.getAttributes());
        String email = info.getEmail();
        boolean verified = info.isEmailVerified();

        if ("github".equals(provider)) {
            email = gitHubEmailFetcher.fetchPrimaryVerifiedEmail(
                    userRequest.getAccessToken().getTokenValue());
            verified = email != null;
        }

        if (info.getId() == null || info.getId().isBlank() || email == null
                || email.isBlank() || !verified) {
            throw oauthError("verified_email_required",
                    "A verified email address is required for OAuth2 login");
        }

        email = email.trim().toLowerCase(Locale.ROOT);
        User user = findOrCreateUser(provider, info, email);
        if (!user.isEnabled()) {
            throw oauthError("account_disabled", "This account is disabled");
        }

        Map<String, Object> attributes = new HashMap<>(providerUser.getAttributes());
        attributes.put("userId", user.getId().toString());
        attributes.put("role", user.getRole().name());
        attributes.put("email", user.getEmail());

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOAuth2User(providerUser.getAuthorities(), attributes, nameAttributeKey);
    }

    private User findOrCreateUser(String provider, OAuth2UserInfo info, String email) {
        Optional<User> byProvider = userRepository.findByOauthProviderAndOauthId(provider, info.getId());
        if (byProvider.isPresent()) {
            return byProvider.get();
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            if (user.getOauthProvider() != null
                    && (!provider.equals(user.getOauthProvider()) || !info.getId().equals(user.getOauthId()))) {
                throw oauthError("oauth_account_conflict",
                        "This email is already associated with another sign-in method");
            }

            user.setOauthProvider(provider);
            user.setOauthId(info.getId());
            user.setEmailVerified(true);
            return userRepository.save(user);
        }

        User user = new User(email, null, info.getFirstName(), info.getLastName(), Role.ROLE_USER);
        user.setOauthProvider(provider);
        user.setOauthId(info.getId());
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private OAuth2AuthenticationException oauthError(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), description);
    }
}
