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
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = {CategoryApplication.class, CategoryIntegrationTest.TestConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
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
                .body("parentId", equalTo(parentId.intValue()));
    }

    @Test
    void whenGetCategoryTree_thenReturnRecursiveStructure() {
        CategoryDto root = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
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

        CategoryDto child = new CategoryDto(null, "Laptops", "Laptop computers", rootId, null);
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

        CategoryDto grandchild = new CategoryDto(null, "Gaming Laptops", "High performance laptops", childId, null);
        given()
                .contentType("application/json")
                .body(grandchild)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);

        given()
                .when()
                .get("/categories/tree/{rootId}", rootId)
                .then()
                .statusCode(200)
                .body("id", equalTo(rootId.intValue()))
                .body("name", equalTo("Electronics"))
                .body("children.size()", equalTo(1))
                .body("children[0].id", equalTo(childId.intValue()))
                .body("children[0].name", equalTo("Laptops"))
                .body("children[0].children.size()", equalTo(1))
                .body("children[0].children[0].name", equalTo("Gaming Laptops"));
    }

    @Test
    void whenGetRootCategories_thenReturnRootsWithChildren() {
        CategoryDto root1 = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
        String root1Location = given()
                .contentType("application/json")
                .body(root1)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long root1Id = Long.valueOf(root1Location.substring(root1Location.lastIndexOf('/') + 1));

        CategoryDto root2 = new CategoryDto(null, "Clothing", "Apparel", null, null);
        given()
                .contentType("application/json")
                .body(root2)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);

        CategoryDto child = new CategoryDto(null, "Laptops", "Laptop computers", root1Id, null);
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
                .body("size()", equalTo(2))
                .body("find { it.name == 'Electronics' }.children.size()", equalTo(1))
                .body("find { it.name == 'Clothing' }.children.size()", equalTo(0));
    }

    @Test
    void whenMoveSubtree_thenUpdateParentAndPreserveChildren() {
        CategoryDto root = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
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

        CategoryDto child = new CategoryDto(null, "Laptops", "Laptop computers", rootId, null);
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

        CategoryDto grandchild = new CategoryDto(null, "Gaming Laptops", "High performance laptops", childId, null);
        String grandchildLocation = given()
                .contentType("application/json")
                .body(grandchild)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long grandchildId = Long.valueOf(grandchildLocation.substring(grandchildLocation.lastIndexOf('/') + 1));

        CategoryDto newRoot = new CategoryDto(null, "Computers", "Computer devices", null, null);
        String newRootLocation = given()
                .contentType("application/json")
                .body(newRoot)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long newRootId = Long.valueOf(newRootLocation.substring(newRootLocation.lastIndexOf('/') + 1));

        given()
                .contentType("application/json")
                .when()
                .post("/categories/{id}/move?newParentId={newParentId}", childId, newRootId)
                .then()
                .statusCode(200)
                .body("id", equalTo(childId.intValue()))
                .body("parentId", equalTo(newRootId.intValue()));

        given()
                .when()
                .get("/categories/tree/{rootId}", newRootId)
                .then()
                .statusCode(200)
                .body("children.size()", equalTo(1))
                .body("children[0].id", equalTo(childId.intValue()))
                .body("children[0].children.size()", equalTo(1))
                .body("children[0].children[0].id", equalTo(grandchildId.intValue()));
    }

    @Test
    void whenMoveCategoryUnderItself_thenReturn400() {
        CategoryDto root = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
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

        given()
                .when()
                .post("/categories/{id}/move?newParentId={newParentId}", rootId, rootId)
                .then()
                .statusCode(400);
    }

    @Test
    void whenMoveCategoryUnderDescendant_thenReturn400CycleDetection() {
        CategoryDto root = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
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

        CategoryDto child = new CategoryDto(null, "Laptops", "Laptop computers", rootId, null);
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
                .post("/categories/{id}/move?newParentId={newParentId}", rootId, childId)
                .then()
                .statusCode(400);
    }

    @Test
    void whenDeleteWithCascade_thenReparentChildrenToParent() {
        CategoryDto root = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
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

        CategoryDto child = new CategoryDto(null, "Laptops", "Laptop computers", rootId, null);
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

        CategoryDto grandchild = new CategoryDto(null, "Gaming Laptops", "High performance laptops", childId, null);
        String grandchildLocation = given()
                .contentType("application/json")
                .body(grandchild)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long grandchildId = Long.valueOf(grandchildLocation.substring(grandchildLocation.lastIndexOf('/') + 1));

        given()
                .when()
                .delete("/categories/{id}/cascade", childId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/categories/tree/{rootId}", rootId)
                .then()
                .statusCode(200)
                .body("children.size()", equalTo(1))
                .body("children[0].id", equalTo(grandchildId.intValue()))
                .body("children[0].parentId", equalTo(rootId.intValue()));
    }

    @Test
    void whenDeleteRootWithCascade_thenChildrenBecomeRoots() {
        CategoryDto root = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
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

        CategoryDto child1 = new CategoryDto(null, "Laptops", "Laptop computers", rootId, null);
        String child1Location = given()
                .contentType("application/json")
                .body(child1)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long child1Id = Long.valueOf(child1Location.substring(child1Location.lastIndexOf('/') + 1));

        CategoryDto child2 = new CategoryDto(null, "Phones", "Mobile phones", rootId, null);
        given()
                .contentType("application/json")
                .body(child2)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);

        given()
                .when()
                .delete("/categories/{id}/cascade", rootId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/categories/tree")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("find { it.id == " + child1Id + " }.parentId", nullValue());
    }

    @Test
    void whenDeleteCategory_thenReturn204() {
        CategoryDto category = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
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
                .delete("/categories/{id}", id)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/categories/{id}", id)
                .then()
                .statusCode(404);
    }

    @Test
    void whenUpdateCategory_thenReturnUpdatedCategory() {
        CategoryDto category = new CategoryDto(null, "Electronics", "Electronic devices", null, null);
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

        CategoryDto updated = new CategoryDto(null, "Consumer Electronics", "Consumer electronic devices", null, null);
        given()
                .contentType("application/json")
                .body(updated)
                .when()
                .put("/categories/{id}", id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Consumer Electronics"))
                .body("description", equalTo("Consumer electronic devices"));
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
