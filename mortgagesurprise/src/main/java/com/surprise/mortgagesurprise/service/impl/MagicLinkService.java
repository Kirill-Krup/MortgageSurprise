package com.surprise.mortgagesurprise.service.impl;

import com.surprise.mortgagesurprise.exception.BusinessException;
import com.surprise.mortgagesurprise.messaging.event.EmailNotificationEvent;
import com.surprise.mortgagesurprise.messaging.producer.MessageProducer;
import com.surprise.mortgagesurprise.model.entity.Role;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.repository.jpa.UserRepository;
import com.surprise.mortgagesurprise.util.Sha256Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class MagicLinkService {

    private static final Logger log = LoggerFactory.getLogger(MagicLinkService.class);

    private static final String KEY_PREFIX = "auth:magic-link:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration GRACE_PERIOD = Duration.ofMinutes(2);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final MessageProducer messageProducer;
    private final String frontendUrl;

    public MagicLinkService(UserRepository userRepository,
                            StringRedisTemplate redisTemplate,
                            MessageProducer messageProducer,
                            @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.messageProducer = messageProducer;
        this.frontendUrl = frontendUrl;
    }

    public void requestLink(String emailAddress) {
        if (emailAddress == null || emailAddress.isBlank()) {
            return;
        }
        String email = emailAddress.trim().toLowerCase(Locale.ROOT);
        sendLink(email);
    }

    @Transactional
    public User consume(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Invalid or expired magic link");
        }

        String hashedTokenKey = key(token);
        String consumedKey = hashedTokenKey + ":consumed";

        // If this token was recently verified within the last 2 minutes (e.g. React StrictMode,
        // multiple clicks, browser prefetching), return the user instead of throwing an error
        String consumedUserId = redisTemplate.opsForValue().get(consumedKey);
        if (consumedUserId != null && !consumedUserId.isBlank()) {
            log.info("Magic link token was recently consumed (grace period active) for userId {}", consumedUserId);
            return userRepository.findById(UUID.fromString(consumedUserId))
                    .orElseThrow(() -> new BusinessException("User not found"));
        }

        String email = redisTemplate.opsForValue().getAndDelete(hashedTokenKey);
        if (email == null) {
            log.warn("Magic link token not found or already expired in Redis");
            throw new BusinessException("Invalid or expired magic link");
        }

        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    if (!existingUser.isEnabled()) {
                        throw new BusinessException("User account is disabled");
                    }
                    if (!existingUser.isEmailVerified()) {
                        existingUser.setEmailVerified(true);
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    User newUser = new User(email, null, null, null, Role.ROLE_USER);
                    newUser.setEmailVerified(true);
                    newUser.setEnabled(true);
                    return userRepository.save(newUser);
                });

        // Set grace period of 2 minutes so double-clicks, React StrictMode, or page reloads don't fail
        redisTemplate.opsForValue().set(consumedKey, user.getId().toString(), GRACE_PERIOD);

        return user;
    }

    private void sendLink(String email) {
        byte[] randomToken = new byte[32];
        SECURE_RANDOM.nextBytes(randomToken);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        redisTemplate.opsForValue().set(key(token), email, TOKEN_TTL);

        String link = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/magic-link")
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();

        String body = String.format(
                "Здравствуйте!\n\n" +
                "Для входа в личный кабинет MortgageSurprise перейдите по ссылке:\n%s\n\n" +
                "Ссылка действительна в течение 30 минут.\n" +
                "Если вы не запрашивали ссылку, просто проигнорируйте это письмо.\n\n" +
                "Команда MortgageSurprise",
                link
        );

        messageProducer.sendEmailNotification(new EmailNotificationEvent(
                email,
                "Вход в личный кабинет MortgageSurprise (Magic Link)",
                body,
                "MAGIC_LINK",
                Instant.now()));
    }

    private String key(String token) {
        return KEY_PREFIX + Sha256Encoder.toSha256(token);
    }
}
