package com.example.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@Tag(name = "Product", description = "Product management APIs")
public class ProductController {

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    public ProductController(DiscoveryClient discoveryClient, RestClient.Builder restClientBuilder) {
        this.discoveryClient = discoveryClient;
        this.restClient = restClientBuilder.build();
    }

    @GetMapping("/")
    @Operation(summary = "Health check", description = "Returns a simple message to confirm the service is running")
    public String helloWorld() {
        return "Product service is running.";
    }

    @GetMapping("/api/products")
    @Operation(summary = "List all products", description = "Returns a list of all products")
    public List<Product> getAllProducts() {
        // In a real app, this would come from a repository
        return List.of(
                new Product(1L, "Laptop", "High-performance laptop", 999.99),
                new Product(2L, "Phone", "Latest smartphone", 699.99)
        );
    }

    @GetMapping("/api/products/category/{categoryId}")
    @Operation(summary = "Get products by category", description = "Calls Category service to get products for a category")
    public ResponseEntity<String> getProductsByCategory(Long categoryId) {
        List<ServiceInstance> instances = discoveryClient.getInstances("category");
        if (instances.isEmpty()) {
            return ResponseEntity.status(503).body("Category service unavailable");
        }
        
        String url = instances.get(0).getUri() + "/categories/" + categoryId;
        String category = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
        
        return ResponseEntity.ok("Products for category: " + category);
    }
}