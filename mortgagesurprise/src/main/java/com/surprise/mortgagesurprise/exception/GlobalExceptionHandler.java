package com.surprise.mortgagesurprise.exception;

import com.surprise.mortgagesurprise.dto.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        var pd = problem(ex.getCode());
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var pd = problem(ErrorCode.VALIDATION_ERROR);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setDetail("Validation failed for one or more fields");

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        var pd = problem(ErrorCode.INVALID_CREDENTIALS);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex, HttpServletRequest request) {
        var pd = problem(ErrorCode.ACCOUNT_DISABLED);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setDetail("User account is disabled");
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        var pd = problem(ErrorCode.ACCESS_DENIED);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        var pd = problem(ErrorCode.NOT_FOUND);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        var pd = problem(ErrorCode.INTERNAL_ERROR);
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    private static ProblemDetail problem(ErrorCode code) {
        var pd = ProblemDetail.forStatus(code.status);
        pd.setTitle(code.status.getReasonPhrase());
        pd.setProperty("code", code.name());
        return pd;
    }
}
