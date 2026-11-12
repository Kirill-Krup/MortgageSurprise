package com.surprise.mortgagesurprise.interceptor;

import tools.jackson.databind.ObjectMapper;
import com.surprise.mortgagesurprise.exception.ErrorResponse;
import com.surprise.mortgagesurprise.service.RateLimiterRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Test
    @DisplayName("GET requests bypass rate limiter")
    void testGetRequestsBypassRateLimiter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/calculator/calculate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verifyNoInteractions(rateLimiterService);
    }

    @Test
    @DisplayName("Allowed POST request passes preHandle")
    void testAllowedPostRequestPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/calculator/calculate");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.allowRequest(eq("ip:192.168.1.100"), eq("calculate"))).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Rejected POST request returns 429 status code and ErrorResponse")
    void testRejectedPostRequestReturns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/calculator/calculate");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.allowRequest(eq("ip:10.0.0.5"), eq("calculate"))).thenReturn(false);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());

        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(429, errorResponse.status());
        assertEquals("Too Many Requests", errorResponse.error());
        assertEquals("/api/v1/calculator/calculate", errorResponse.path());
    }
}
