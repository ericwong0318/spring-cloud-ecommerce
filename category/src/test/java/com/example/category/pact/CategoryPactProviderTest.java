package com.example.category.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.State;
import com.example.category.Category;
import com.example.category.CategoryRepository;
import com.example.category.TestSecurityConfig;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = com.example.category.CategoryApplication.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class, org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration.class})
@Import({TestSecurityConfig.class, CategoryPactProviderTest.TestConfig.class})
@ExtendWith(CategoryPactExtension.class)
class CategoryPactProviderTest {

    @LocalServerPort
    private int port;

    @MockBean
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("category with id 123 exists")
    void categoryWithId123Exists(Map<String, Object> params) {
        new TransactionTemplate(transactionManager).execute(status -> {
            // Create root category with id 0 first
            entityManager.createNativeQuery("INSERT INTO categories (id, name, description, parent_id) VALUES (0, 'Root', 'Root category', NULL)")
                    .executeUpdate();
            // Create category with id 123 referencing root
            entityManager.createNativeQuery("INSERT INTO categories (id, name, description, parent_id) VALUES (123, 'Electronics', 'Electronic devices and accessories', 0)")
                    .executeUpdate();
            return null;
        });
    }

    @State("valid category create request")
    void validCategoryCreateRequest(Map<String, Object> params) {
    }

    @State("valid category update request")
    void validCategoryUpdateRequest(Map<String, Object> params) {
    }

    @State("category create request - name blank")
    void categoryCreateRequestNameBlank(Map<String, Object> params) {
    }

    @State("category create request - name null")
    void categoryCreateRequestNameNull(Map<String, Object> params) {
    }

    @State("category create request - name too long")
    void categoryCreateRequestNameTooLong(Map<String, Object> params) {
    }

    @State("category create request - description too long")
    void categoryCreateRequestDescriptionTooLong(Map<String, Object> params) {
    }

    @State("category update request - name blank")
    void categoryUpdateRequestNameBlank(Map<String, Object> params) {
    }

    @State("category update request - name null")
    void categoryUpdateRequestNameNull(Map<String, Object> params) {
    }

    @State("category update request - name too long")
    void categoryUpdateRequestNameTooLong(Map<String, Object> params) {
    }

    @State("category update request - description too long")
    void categoryUpdateRequestDescriptionTooLong(Map<String, Object> params) {
    }

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        OutboxEventPublisher outboxEventPublisher(OutboxEventRepository outboxEventRepository) {
            return new OutboxEventPublisher(outboxEventRepository, null, new ObjectMapper()) {
                @Override
                public void saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
                }
            };
        }
    }
}