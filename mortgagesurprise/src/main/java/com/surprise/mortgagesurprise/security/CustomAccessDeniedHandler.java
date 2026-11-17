package com.surprise.mortgagesurprise.security;

import com.surprise.mortgagesurprise.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        var message = isMessageExists(accessDeniedException)
                ? "Access denied: " + accessDeniedException.getMessage()
                : "Access denied: You do not have permission to access this resource";
        var errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private boolean isMessageExists(AccessDeniedException accessDeniedException) {
        return accessDeniedException != null && accessDeniedException.getMessage() != null;
    }
}
