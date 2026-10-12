package com.example.product.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.State;
import com.example.product.CategoryClient;
import com.example.product.ProductApplication;
import com.example.product.ProductRepository;
import com.example.product.ProductMapper;
import com.example.product.TestSecurityConfig;
import com.example.product.config.RabbitMQConfig;
import com.example.common.dto.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.web-application-type=reactive",
        "eureka.client.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
    }
)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    ReactiveSecurityAutoConfiguration.class,
    ReactiveOAuth2ResourceServerAutoConfiguration.class,
    ReactiveManagementWebSecurityAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("product-service")
@PactFolder("../order-service/target/pacts")
class ProductPactProviderTest {

    @LocalServerPort
    private int port;

    @MockBean
    private CategoryClient categoryClient;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private ProductMapper productMapper;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        Mockito.reset(categoryClient, productRepository, productMapper);
        Mockito.when(categoryClient.getCategoryName(123L)).thenReturn(Optional.of("Electronics"));
    }

    @TestTemplate
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("valid product create request")
    void validProductCreateRequest() {
        // Mock the category name lookup for "cat-123" -> extracts "123"
        Mockito.when(categoryClient.getCategoryName(123L)).thenReturn(Optional.of("Electronics"));

        // Create a product entity that would be saved
        com.example.product.Product entity = new com.example.product.Product();
        entity.setName("Laptop Pro 15");
        entity.setDescription("High-performance laptop for professionals");
        entity.setCategoryId("cat-123");
        entity.setCategoryName("Electronics");
        entity.setPrice(new java.math.BigDecimal("1999.99"));

        com.example.product.Product savedEntity = new com.example.product.Product();
        savedEntity.setId("1");
        savedEntity.setName("Laptop Pro 15");
        savedEntity.setDescription("High-performance laptop for professionals");
        savedEntity.setCategoryId("cat-123");
        savedEntity.setCategoryName("Electronics");
        savedEntity.setPrice(new java.math.BigDecimal("1999.99"));

        ProductDto responseDto = new ProductDto(
            "1", "Laptop Pro 15", "High-performance laptop for professionals",
            new java.math.BigDecimal("1999.99"), "cat-123", "Electronics", java.util.List.of()
        );

        // Use argument matchers to match any ProductDto passed to mapper
        Mockito.when(productMapper.toEntity(Mockito.any(ProductDto.class))).thenReturn(entity);
        Mockito.when(productRepository.save(entity)).thenReturn(Mono.just(savedEntity));
        Mockito.when(productMapper.toDto(savedEntity)).thenReturn(responseDto);
    }

    @State("valid product update request")
    void validProductUpdateRequest() {
    }

    @State("product create request - name blank")
    void productCreateRequestNameBlank() {
    }

    @State("product create request - name null")
    void productCreateRequestNameNull() {
    }

    @State("product create request - name too long")
    void productCreateRequestNameTooLong() {
    }

    @State("product create request - description too long")
    void productCreateRequestDescriptionTooLong() {
    }

    @State("product create request - price null")
    void productCreateRequestPriceNull() {
    }

    @State("product create request - price not positive")
    void productCreateRequestPriceNotPositive() {
    }

    @State("product create request - price negative")
    void productCreateRequestPriceNegative() {
    }

    @State("product update request - name blank")
    void productUpdateRequestNameBlank() {
    }

    @State("product update request - name null")
    void productUpdateRequestNameNull() {
    }

    @State("product update request - name too long")
    void productUpdateRequestNameTooLong() {
    }

    @State("product update request - description too long")
    void productUpdateRequestDescriptionTooLong() {
    }

    @State("product update request - price null")
    void productUpdateRequestPriceNull() {
    }

    @State("product update request - price not positive")
    void productUpdateRequestPriceNotPositive() {
    }
}