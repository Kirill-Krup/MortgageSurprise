package com.surprise.mortgagesurprise.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenTelemetry logging integration.
 * Installs the active OpenTelemetry SDK instance into the Logback OpenTelemetryAppender,
 * ensuring all application log events are enriched with traceId/spanId and exported via OTLP.
 */
@Configuration(proxyBeanMethods = false)
public class OpenTelemetryConfig {

    private final OpenTelemetry openTelemetry;

    public OpenTelemetryConfig(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @PostConstruct
    public void init() {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
