package com.nexaforge.workflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI workflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Workflow Orchestration Platform API")
                        .description("Production-style distributed workflow orchestration with "
                                + "state machine lifecycle, idempotent commands, Kafka event processing, "
                                + "compensation/rollback, and observability.")
                        .version("1.0.0")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                        .contact(new Contact()
                                .name("NexaForge Engineering")
                                .url("https://github.com/shariquefaizi94-beep/workflow-orchestration-platform")));
    }
}
