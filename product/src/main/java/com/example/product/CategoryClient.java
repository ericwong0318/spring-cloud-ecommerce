package com.example.product;

import com.example.common.dto.CategoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class CategoryClient {

    private static final Logger log = LoggerFactory.getLogger(CategoryClient.class);

    private final RestClient restClient;

    public CategoryClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://category")
                .build();
    }

    public Optional<String> getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return Optional.empty();
        }
        try {
            CategoryDto category = restClient.get()
                    .uri("/categories/{id}", categoryId)
                    .retrieve()
                    .body(CategoryDto.class);
            return Optional.ofNullable(category)
                    .map(CategoryDto::name);
        } catch (Exception e) {
            log.warn("Failed to fetch category name for id {}: {}", categoryId, e.getMessage());
            return Optional.empty();
        }
    }
}