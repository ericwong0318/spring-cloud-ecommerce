package com.example.category.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.junitsupport.State;
import com.example.category.TestSecurityConfig;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.OutboxEventRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Map;

import static kotlin.Unit.INSTANCE;

@DataJpaTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class})
@Import({com.example.category.TestSecurityConfig.class, CategoryPactProviderTest.TestConfig.class})
@ExtendWith(PactVerificationInvocationContextProvider.class)
@au.com.dius.pact.provider.junitsupport.Provider("category-service")
@au.com.dius.pact.provider.junitsupport.loader.PactFolder("../product/target/pacts")
class CategoryPactProviderTest {

    @LocalServerPort
    private int port;

    @MockBean
    private OutboxEventRepository outboxEventRepository;

    private static ProviderInfo providerInfo;

    @BeforeAll
    static void setupProvider() {
        providerInfo = new ProviderInfo("category-service");
        providerInfo.setProtocol("http");
        providerInfo.setHost("localhost");
        providerInfo.setPath("/");

        providerInfo.hasPactWith("product-service", consumer -> {
            consumer.setPactSource(new File("../product/target/pacts"));
            return INSTANCE;
        });
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        context.setProviderInfo(providerInfo);
    }

    @TestTemplate
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @au.com.dius.pact.provider.junitsupport.State("valid category create request")
    void validCategoryCreateRequest(Map<String, Object> params) {
    }

    @au.com.dius.pact.provider.junitsupport.State("valid category update request")
    void validCategoryUpdateRequest(Map<String, Object> params) {
    }

    @au.com.dius.pact.provider.junitsupport.State("category create request - name blank")
    void categoryCreateRequestNameBlank(Map<String, Object> params) {
    }

    @au.com.dius.pact.provider.junitsupport.State("category create request - name null")
    void categoryCreateRequestNameNull(Map<String, Object> params) {
    }

    @au.com.dius.pact.provider.junitsupport.State("category create request - name too long")
    void categoryCreateRequestNameTooLong(Map<String, Object> params) {
    }

    @au.com.dius.pact.provider.junitsupport.State("category create request - description too long")
    void categoryCreateRequestDescriptionTooLong(Map<String, Object> params) {
    }

    @au.com.dius.pact.provider.junitsupport.State("category update request - name blank")
    void categoryUpdateRequestNameBlank(Map<String, Object> params) {
    }

    @au.com.dius.pact.provider.junitsupport.State("category update request - name null")
    void categoryUpdateRequestNameNull(Map<String, Object> params) {
    }

    @au.com.dius.pact.provider.junitsupport.State("category update request - name too long")
    void categoryUpdateRequestNameTooLong(Map<String, Object> params) {
    }

    @au.com.dius.pact.provider.junitsupport.State("category update request - description too long")
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