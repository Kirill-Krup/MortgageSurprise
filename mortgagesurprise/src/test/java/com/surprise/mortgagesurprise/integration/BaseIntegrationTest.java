package com.surprise.mortgagesurprise.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @ServiceConnection
    public static final PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("mortgagesurprise_test")
            .withUsername("test")
            .withPassword("test");

    @ServiceConnection
    public static final MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @ServiceConnection
    public static final RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"));
}