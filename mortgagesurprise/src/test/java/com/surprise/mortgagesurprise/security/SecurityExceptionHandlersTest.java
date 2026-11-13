package com.surprise.mortgagesurprise.security;

import tools.jackson.databind.ObjectMapper;
import com.surprise.mortgagesurprise.exception.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    @DisplayName("AuthenticationEntryPoint returns 401 with ErrorResponse JSON")
    void testAuthenticationEntryPointReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/scenarios");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(request, response, new BadCredentialsException("Token is expired"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());

        ErrorResponse error = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(401, error.status());
        assertEquals("Unauthorized", error.error());
        assertEquals("Token is expired", error.message());
        assertEquals("/api/v1/scenarios", error.path());
    }

    @Test
    @DisplayName("AccessDeniedHandler returns 403 with ErrorResponse JSON")
    void testAccessDeniedHandlerReturns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/bank-offers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Insufficient authorities"));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());

        ErrorResponse error = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(403, error.status());
        assertEquals("Forbidden", error.error());
        assertEquals("Access denied: Insufficient authorities", error.message());
        assertEquals("/api/v1/admin/bank-offers", error.path());
    }
}
