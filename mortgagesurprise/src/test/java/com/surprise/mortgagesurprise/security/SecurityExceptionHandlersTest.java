package com.surprise.mortgagesurprise.security;

import tools.jackson.databind.ObjectMapper;
import com.surprise.mortgagesurprise.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Tag("unit")
@DisplayName("Security Exception Handlers Tests")
class SecurityExceptionHandlersTest {

    private ObjectMapper objectMapper;
    private CustomAuthenticationEntryPoint authenticationEntryPoint;
    private CustomAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authenticationEntryPoint = new CustomAuthenticationEntryPoint(objectMapper);
        accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper);
    }

    @Nested
    @DisplayName("Authentication Entry Point")
    class AuthenticationEntryPointTests {

        @Test
        @DisplayName("Should return 401 Unauthorized with structured ErrorResponse JSON")
        void commence_shouldReturn401Unauthorized_whenAuthenticationFails() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/scenarios");
            MockHttpServletResponse response = new MockHttpServletResponse();
            BadCredentialsException exception = new BadCredentialsException("Token is expired");

            // Act
            authenticationEntryPoint.commence(request, response, exception);

            // Assert
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

            ErrorResponse error = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
            assertAll("401 Error response fields",
                    () -> assertThat(error.status()).isEqualTo(401),
                    () -> assertThat(error.error()).isEqualTo("Unauthorized"),
                    () -> assertThat(error.message()).isEqualTo("Token is expired"),
                    () -> assertThat(error.path()).isEqualTo("/api/v1/scenarios")
            );
        }
    }

    @Nested
    @DisplayName("Access Denied Handler")
    class AccessDeniedHandlerTests {

        @Test
        @DisplayName("Should return 403 Forbidden with structured ErrorResponse JSON")
        void handle_shouldReturn403Forbidden_whenAccessDenied() throws Exception {
            // Arrange
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/bank-offers");
            MockHttpServletResponse response = new MockHttpServletResponse();
            AccessDeniedException exception = new AccessDeniedException("Insufficient authorities");

            // Act
            accessDeniedHandler.handle(request, response, exception);

            // Assert
            assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

            ErrorResponse error = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
            assertAll("403 Error response fields",
                    () -> assertThat(error.status()).isEqualTo(403),
                    () -> assertThat(error.error()).isEqualTo("Forbidden"),
                    () -> assertThat(error.message()).isEqualTo("Access denied: Insufficient authorities"),
                    () -> assertThat(error.path()).isEqualTo("/api/v1/admin/bank-offers")
            );
        }
    }
}
