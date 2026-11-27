package com.surprise.mortgagesurprise.integration;

import tools.jackson.databind.ObjectMapper;
import com.surprise.mortgagesurprise.dto.auth.AuthResponse;
import com.surprise.mortgagesurprise.dto.auth.LoginRequest;
import com.surprise.mortgagesurprise.dto.auth.RefreshTokenRequest;
import com.surprise.mortgagesurprise.dto.auth.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@AutoConfigureMockMvc
@DisplayName("Auth Flow End-to-End Integration Tests")
class AuthFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Complete Authentication Lifecycle")
    class LifecycleTests {

        @Test
        @DisplayName("Should execute register -> login -> refresh token lifecycle seamlessly")
        void authenticate_shouldCompleteFullAuthLifecycle_whenRegisterLoginAndRefreshInvoked() throws Exception {
            String email = "testuser" + System.currentTimeMillis() + "@example.com";
            String password = "StrongPassword123";

            RegisterRequest registerReq = new RegisterRequest(email, password, "Ivan", "Ivanov");
            MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            AuthResponse regResponse = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);

            assertAll("Register response tokens",
                    () -> assertThat(regResponse.accessToken()).isNotBlank(),
                    () -> assertThat(regResponse.refreshToken()).isNotBlank(),
                    () -> assertThat(regResponse.email()).isEqualTo(email)
            );

            LoginRequest loginReq = new LoginRequest(email, password);
            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

            assertThat(loginResponse.accessToken()).isNotBlank();
            String refreshToken = loginResponse.refreshToken();
            assertThat(refreshToken).isNotBlank();

            RefreshTokenRequest refreshReq = new RefreshTokenRequest(refreshToken);
            MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthResponse refreshResponse = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), AuthResponse.class);

            assertThat(refreshResponse.accessToken()).isNotBlank();
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when invalid credentials are provided for login")
        void login_shouldReturnUnauthorized_whenInvalidCredentialsSupplied() throws Exception {
            LoginRequest invalidLogin = new LoginRequest("nonexistent@example.com", "WrongPassword123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidLogin)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
