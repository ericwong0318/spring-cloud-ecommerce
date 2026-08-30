package com.example.category;

import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.OutboxEventRepository;
import com.example.common.dto.CategoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.mock.mockito.MockBean;

import static io.restassured.RestAssured.given;

@SpringBootTest(classes = {CategoryApplication.class, CategoryIntegrationTest.TestConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
class CategoryIntegrationTest {

    @LocalServerPort
    int port;

    @MockBean
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        io.restassured.RestAssured.baseURI = "http://localhost:" + port;
    }

    @Test
    void whenAllCategoriesRetrieved_thenReturn200() {
        given()
                .when()
                .get("/categories")
                .then()
                .statusCode(200);
    }

    @Test
    void whenCategoryCreated_thenReturn201() {
        CategoryDto category = new CategoryDto(
                null, "Electronics", "Electronic devices", null, null
        );

        given()
                .contentType("application/json")
                .body(category)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);
    }

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        OutboxEventPublisher outboxEventPublisher(OutboxEventRepository outboxEventRepository) {
            return new OutboxEventPublisher(outboxEventRepository, null, new ObjectMapper()) {
                @Override
                public void saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
                    // no-op for tests
                }
            };
        }
    }
}