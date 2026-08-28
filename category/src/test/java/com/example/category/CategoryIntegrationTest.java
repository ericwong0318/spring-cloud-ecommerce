package com.example.category;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CategoryIntegrationTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
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
    void whenCategoryCreated_thenReturn200() {
        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("Electronic devices");

        given()
                .contentType("application/json")
                .body(category)
                .when()
                .post("/categories")
                .then()
                .statusCode(200);
    }
}