package com.surprise.mortgagesurprise.interceptor;

import tools.jackson.databind.ObjectMapper;
import com.surprise.mortgagesurprise.security.CustomUserDetails;
import com.surprise.mortgagesurprise.service.RateLimiterRedisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptor Tests")
class RateLimitInterceptorTest {

    @Mock
    private RateLimiterRedisService rateLimiterService;

    private ObjectMapper objectMapper;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        interceptor = new RateLimitInterceptor(rateLimiterService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("HTTP Method Filtering")
    class MethodFiltering {

        @Test
        @DisplayName("Should bypass rate limiter for non-POST HTTP requests (e.g., GET)")
        void preHandle_shouldBypassRateLimiter_whenRequestMethodIsGet() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/calculator/calculate");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Act
            boolean result = interceptor.preHandle(request, response, new Object());

            // Assert
            assertThat(result).isTrue();
            verifyNoInteractions(rateLimiterService);
        }
    }

    @Nested
    @DisplayName("Request Allowance and Rejection")
    class RequestAllowance {

        @Test
        @DisplayName("Should allow POST request when rate limiter returns true")
        void preHandle_shouldAllowRequest_whenLimitNotExceeded() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/calculator/calculate");
            request.setRemoteAddr("192.168.1.100");
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(rateLimiterService.allowRequest(eq("ip:192.168.1.100"), eq("calculate"))).thenReturn(true);

            // Act
            boolean result = interceptor.preHandle(request, response, new Object());

            // Assert
            assertThat(result).isTrue();
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        @Test
        @DisplayName("Should reject POST request with 429 TOO_MANY_REQUESTS when limit exceeded")
        void preHandle_shouldRejectRequestWith429_whenLimitExceeded() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/calculator/calculate");
            request.setRemoteAddr("10.0.0.5");
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(rateLimiterService.allowRequest(eq("ip:10.0.0.5"), eq("calculate"))).thenReturn(false);

            // Act
            boolean result = interceptor.preHandle(request, response, new Object());

            // Assert
            assertThat(result).isFalse();
            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

            org.springframework.http.ProblemDetail errorResponse = objectMapper.readValue(response.getContentAsString(), org.springframework.http.ProblemDetail.class);
            assertThat(errorResponse.getStatus()).isEqualTo(429);
            assertThat(errorResponse.getTitle()).isEqualTo("Too Many Requests");
            assertThat(errorResponse.getInstance()).isEqualTo(java.net.URI.create("/api/v1/calculator/calculate"));
            assertThat(errorResponse.getProperties().get("code")).isEqualTo("RATE_LIMIT_EXCEEDED");
        }
    }

    @Nested
    @DisplayName("Client Identification Strategy")
    class ClientIdentification {

        @Test
        @DisplayName("Should identify authenticated user by userId prefix")
        void preHandle_shouldIdentifyByUserId_whenUserIsAuthenticated() throws Exception {
            // Arrange
            UUID userId = UUID.randomUUID();
            com.surprise.mortgagesurprise.model.entity.User user = new com.surprise.mortgagesurprise.model.entity.User();
            user.setId(userId);
            user.setEmail("user@example.com");
            user.setPassword("secret");
            user.setRole(com.surprise.mortgagesurprise.model.entity.Role.ROLE_USER);
            CustomUserDetails userDetails = new CustomUserDetails(user);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
            );

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/calculator/calculate");
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(rateLimiterService.allowRequest(eq("user:" + userId), eq("calculate"))).thenReturn(true);

            // Act
            boolean result = interceptor.preHandle(request, response, new Object());

            // Assert
            assertThat(result).isTrue();
            verify(rateLimiterService).allowRequest("user:" + userId, "calculate");
        }

        @Test
        @DisplayName("Should identify anonymous user by X-Forwarded-For header if present")
        void preHandle_shouldIdentifyByXForwardedFor_whenHeaderPresent() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/calculator/calculate");
            request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18");
            MockHttpServletResponse response = new MockHttpServletResponse();
            when(rateLimiterService.allowRequest(eq("ip:203.0.113.195"), eq("calculate"))).thenReturn(true);

            // Act
            boolean result = interceptor.preHandle(request, response, new Object());

            // Assert
            assertThat(result).isTrue();
            verify(rateLimiterService).allowRequest("ip:203.0.113.195", "calculate");
        }
    }
}
