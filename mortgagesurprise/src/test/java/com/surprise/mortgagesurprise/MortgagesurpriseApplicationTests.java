package com.surprise.mortgagesurprise;

import com.surprise.mortgagesurprise.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@DisplayName("MortgagesurpriseApplication Context Loader")
class MortgagesurpriseApplicationTests extends BaseIntegrationTest {

    @Test
    @DisplayName("Application context loads successfully with Testcontainers")
    void contextLoads_shouldStartApplicationSuccessfully_whenContextInitialized() {
        // Arrange & Act & Assert
        assertThat(postgres.isRunning()).isTrue();
        assertThat(mongo.isRunning()).isTrue();
        assertThat(rabbitmq.isRunning()).isTrue();
    }
}
