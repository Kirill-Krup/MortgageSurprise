package com.surprise.mortgagesurprise.integration;

import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import com.redis.testcontainers.RedisContainer;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Base Integration Test")
public abstract class BaseIntegrationTest {

    public static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/price"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "USD/RUB": { "price": "82.50000" },
                                    "EUR/RUB": { "price": "90.20000" },
                                    "BYN/RUB": { "price": "28.40000" }
                                }
                                """)));
    }

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("twelvedata.base-url", wireMockServer::baseUrl);
        registry.add("twelvedata.api-key", () -> "test-api-key");
    }

    @ServiceConnection
    public static final PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("mortgagesurprise_test")
            .withUsername("test")
            .withPassword("test");

    @ServiceConnection
    public static final MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @ServiceConnection
    public static final RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"));

    @ServiceConnection
    public static final MailpitContainer mailpit = new MailpitContainer("axllent/mailpit:v1.24.2");

    @ServiceConnection(name = "redis")
    public static final RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));
}
