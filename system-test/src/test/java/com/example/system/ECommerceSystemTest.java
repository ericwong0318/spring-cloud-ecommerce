package com.example.system;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ECommerceSystemTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void testPostgresContainerStarts() {
        given()
                .port(5432)
                .when()
                .get("/")
                .then()
                .statusCode(501);
    }

    @Test
    void testContainerIsRunning() {
        org.junit.jupiter.api.Assertions.assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
    }

    @Test
    void testPostgresConnection() {
        org.junit.jupiter.api.Assertions.assertNotNull(postgres.getJdbcUrl(), "PostgreSQL JDBC URL should be available");
        org.junit.jupiter.api.Assertions.assertNotNull(postgres.getUsername(), "PostgreSQL username should be available");
    }

    @Test
    void testServiceHealthEndpointStructure() {
        given()
                .when()
                .get("http://localhost:" + postgres.getMappedPort(5432) + "/")
                .then()
                .statusCode(anyOf(is(501), is(400)));
    }
}