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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = {CategoryApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class, R2dbcAutoConfiguration.class, R2dbcDataAutoConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

    @Test
    void whenCreateCategoryWithParent_thenReturn201WithParentId() {
        CategoryDto parent = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
        String parentLocation = given()
                .contentType("application/json")
                .body(parent)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long parentId = Long.valueOf(parentLocation.substring(parentLocation.lastIndexOf('/') + 1));

        CategoryDto child = new CategoryDto(null, "Laptops", "Laptop computers", parentId, null);
        given()
                .contentType("application/json")
                .body(child)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .body("parentId", equalTo(parentId));
    }

    @Test
    void whenDeleteCategory_thenReturn204() {
        CategoryDto category = new CategoryDto(null, "ToDelete", "To be deleted", null, null);
        String location = given()
                .contentType("application/json")
                .body(category)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long id = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        given()
                .when()
                .delete("/categories/" + id)
                .then()
                .statusCode(204);
    }

    @Test
    void whenDeleteRootWithCascade_thenChildrenBecomeRoots() {
        CategoryDto parent = new CategoryDto(null, "Parent", "Parent category", null, null);
        String parentLocation = given()
                .contentType("application/json")
                .body(parent)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long parentId = Long.valueOf(parentLocation.substring(parentLocation.lastIndexOf('/') + 1));

        CategoryDto child = new CategoryDto(null, "Child", "Child category", parentId, null);
        String childLocation = given()
                .contentType("application/json")
                .body(child)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long childId = Long.valueOf(childLocation.substring(childLocation.lastIndexOf('/') + 1));

        given()
                .when()
                .delete("/categories/" + parentId + "?cascade=true")
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/categories/" + childId)
                .then()
                .statusCode(200)
                .body("parentId", nullValue());
    }

    @Test
    void whenDeleteWithCascade_thenReparentChildrenToParent() {
        CategoryDto grandParent = new CategoryDto(null, "GrandParent", "GrandParent category", null, null);
        String gpLocation = given()
                .contentType("application/json")
                .body(grandParent)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long gpId = Long.valueOf(gpLocation.substring(gpLocation.lastIndexOf('/') + 1));

        CategoryDto parent = new CategoryDto(null, "Parent", "Parent category", gpId, null);
        String parentLocation = given()
                .contentType("application/json")
                .body(parent)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long parentId = Long.valueOf(parentLocation.substring(parentLocation.lastIndexOf('/') + 1));

        CategoryDto child = new CategoryDto(null, "Child", "Child category", parentId, null);
        given()
                .contentType("application/json")
                .body(child)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);

        given()
                .when()
                .delete("/categories/" + parentId + "?cascade=true")
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/categories/" + gpId + "/children")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void whenGetCategoryTree_thenReturnRecursiveStructure() {
        CategoryDto parent = new CategoryDto(null, "TreeParent", "Tree parent", null, null);
        String parentLocation = given()
                .contentType("application/json")
                .body(parent)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long parentId = Long.valueOf(parentLocation.substring(parentLocation.lastIndexOf('/') + 1));

        CategoryDto child = new CategoryDto(null, "TreeChild", "Tree child", parentId, null);
        given()
                .contentType("application/json")
                .body(child)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/categories/tree")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void whenGetRootCategories_thenReturnRootsWithChildren() {
        CategoryDto root = new CategoryDto(null, "RootCat", "Root category", null, null);
        String rootLocation = given()
                .contentType("application/json")
                .body(root)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long rootId = Long.valueOf(rootLocation.substring(rootLocation.lastIndexOf('/') + 1));

        CategoryDto child = new CategoryDto(null, "RootChild", "Root child", rootId, null);
        given()
                .contentType("application/json")
                .body(child)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/categories/roots")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void whenMoveCategoryUnderDescendant_thenReturn400CycleDetection() {
        CategoryDto parent = new CategoryDto(null, "MoveParent", "Move parent", null, null);
        String parentLocation = given()
                .contentType("application/json")
                .body(parent)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long parentId = Long.valueOf(parentLocation.substring(parentLocation.lastIndexOf('/') + 1));

        CategoryDto child = new CategoryDto(null, "MoveChild", "Move child", parentId, null);
        String childLocation = given()
                .contentType("application/json")
                .body(child)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long childId = Long.valueOf(childLocation.substring(childLocation.lastIndexOf('/') + 1));

        given()
                .contentType("application/json")
                .body("{\"newParentId\": " + childId + "}")
                .when()
                .put("/categories/" + parentId + "/move")
                .then()
                .statusCode(400);
    }

    @Test
    void whenMoveCategoryUnderItself_thenReturn400() {
        CategoryDto category = new CategoryDto(null, "SelfMove", "Self move", null, null);
        String location = given()
                .contentType("application/json")
                .body(category)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long id = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        given()
                .contentType("application/json")
                .body("{\"newParentId\": " + id + "}")
                .when()
                .put("/categories/" + id + "/move")
                .then()
                .statusCode(400);
    }

    @Test
    void whenMoveSubtree_thenUpdateParentAndPreserveChildren() {
        CategoryDto parent = new CategoryDto(null, "SubtreeParent", "Subtree parent", null, null);
        String parentLocation = given()
                .contentType("application/json")
                .body(parent)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long parentId = Long.valueOf(parentLocation.substring(parentLocation.lastIndexOf('/') + 1));

        CategoryDto child = new CategoryDto(null, "SubtreeChild", "Subtree child", parentId, null);
        String childLocation = given()
                .contentType("application/json")
                .body(child)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long childId = Long.valueOf(childLocation.substring(childLocation.lastIndexOf('/') + 1));

        CategoryDto newParent = new CategoryDto(null, "NewParent", "New parent", null, null);
        String newParentLocation = given()
                .contentType("application/json")
                .body(newParent)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long newParentId = Long.valueOf(newParentLocation.substring(newParentLocation.lastIndexOf('/') + 1));

        given()
                .contentType("application/json")
                .body("{\"newParentId\": " + newParentId + "}")
                .when()
                .put("/categories/" + parentId + "/move")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/categories/" + childId)
                .then()
                .statusCode(200)
                .body("parentId", equalTo(newParentId));
    }

    @Test
    void whenUpdateCategory_thenReturnUpdatedCategory() {
        CategoryDto category = new CategoryDto(null, "Original", "Original description", null, null);
        String location = given()
                .contentType("application/json")
                .body(category)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long id = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        CategoryDto updated = new CategoryDto(null, "Updated", "Updated description", null, null);
        given()
                .contentType("application/json")
                .body(updated)
                .when()
                .put("/categories/" + id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated"))
                .body("description", equalTo("Updated description"));
    }
}
