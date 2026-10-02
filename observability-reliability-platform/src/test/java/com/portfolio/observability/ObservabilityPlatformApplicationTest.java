package com.portfolio.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Context Tests")
class ObservabilityPlatformApplicationTest {

    @Test
    @DisplayName("Should load application context")
    void contextLoads() {
        // Context loads successfully
    }
}
