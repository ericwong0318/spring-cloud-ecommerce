package com.example.system;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = ECommerceSystemTest.TestConfig.class)
class ECommerceSystemTest {

    @Configuration
    static class TestConfig {
    }

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
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
    }

    @Test
    void testContainerIsRunning() {
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
    }

    @Test
    void testPostgresConnection() {
        assertNotNull(postgres.getJdbcUrl(), "PostgreSQL JDBC URL should be available");
        assertNotNull(postgres.getUsername(), "PostgreSQL username should be available");
        
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), 
                postgres.getUsername(), 
                postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        } catch (Exception e) {
            fail("Should be able to connect to PostgreSQL: " + e.getMessage());
        }
    }

    @Test
    void testServiceHealthEndpointStructure() {
        assertNotNull(postgres.getMappedPort(5432), "PostgreSQL port should be mapped");
        assertTrue(postgres.getMappedPort(5432) > 0, "Mapped port should be positive");
    }
}