package com.surprise.mortgagesurprise.security;

import com.surprise.mortgagesurprise.model.entity.Role;
import com.surprise.mortgagesurprise.model.entity.User;
import com.surprise.mortgagesurprise.security.jwt.JwtAuthenticationFilter;
import com.surprise.mortgagesurprise.security.jwt.JwtTokenProvider;
import com.surprise.mortgagesurprise.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Blocked User Security & Filtering Tests")
class BlockedUserSecurityTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("CustomUserDetails should report isEnabled as true for Spring Security but isUserEnabled as false when blocked")
    void customUserDetails_blockedStatusReporting() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("blocked@example.com");
        user.setPassword("hash");
        user.setRole(Role.ROLE_USER);
        user.setEnabled(false);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isUserEnabled()).isFalse();
    }

    @Test
    @DisplayName("Blocked user calling protected resource should receive 403 ACCOUNT_DISABLED")
    void blockedUser_callingProtectedResource_shouldReturn403() throws Exception {
        String token = "valid-token";
        String email = "blocked@example.com";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(Role.ROLE_USER);
        user.setEnabled(false);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/calculator/calculate");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateAccessToken(token)).thenReturn(true);
        when(tokenProvider.getEmailFromToken(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ACCOUNT_DISABLED");
        assertThat(response.getContentAsString()).contains(email);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Blocked user calling /auth/me should be allowed through to inspect status")
    void blockedUser_callingAuthMe_shouldBeAllowed() throws Exception {
        String token = "valid-token";
        String email = "blocked@example.com";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(Role.ROLE_USER);
        user.setEnabled(false);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateAccessToken(token)).thenReturn(true);
        when(tokenProvider.getEmailFromToken(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("Active user calling protected resource should authenticate normally")
    void activeUser_callingProtectedResource_shouldAuthenticate() throws Exception {
        String token = "valid-token";
        String email = "active@example.com";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/calculator/calculate");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateAccessToken(token)).thenReturn(true);
        when(tokenProvider.getEmailFromToken(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
