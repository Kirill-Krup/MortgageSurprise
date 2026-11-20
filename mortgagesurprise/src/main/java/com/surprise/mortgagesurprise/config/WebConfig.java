package com.surprise.mortgagesurprise.config;

import com.surprise.mortgagesurprise.interceptor.RateLimitInterceptor;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/calculator/calculate", "/api/v1/calculator/microloan", "/api/v1/calculator/mortgage");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, UserAuditActionType.class, UserAuditActionType::fromString);
    }
}
