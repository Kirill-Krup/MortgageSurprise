package com.idfinance.credit_scoring_service.security;

import com.idfinance.credit_scoring_service.dto.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        var message = isMessageExists(authException) ? authException.getMessage()
                : "Full authentication is required to access this resource";

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message);
        pd.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("code", ErrorCode.INVALID_CREDENTIALS.name());

        response.getWriter().write(objectMapper.writeValueAsString(pd));
    }

    private boolean isMessageExists(AuthenticationException authException) {
        return authException != null && authException.getMessage() != null;
    }
}
