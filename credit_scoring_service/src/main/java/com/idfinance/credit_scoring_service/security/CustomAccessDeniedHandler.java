package com.idfinance.credit_scoring_service.security;

import com.idfinance.credit_scoring_service.dto.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

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

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, message);
        pd.setTitle(HttpStatus.FORBIDDEN.getReasonPhrase());
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("code", ErrorCode.ACCESS_DENIED.name());

        response.getWriter().write(objectMapper.writeValueAsString(pd));
    }

    private boolean isMessageExists(AccessDeniedException accessDeniedException) {
        return accessDeniedException != null && accessDeniedException.getMessage() != null;
    }
}
