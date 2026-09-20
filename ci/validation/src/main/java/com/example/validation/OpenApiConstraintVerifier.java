package com.example.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Verifies that Jakarta Validation annotations on DTO fields
 * have corresponding OpenAPI schema constraints.
 */
public class OpenApiConstraintVerifier {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    // Mapping from validation annotation to expected OpenAPI constraints
    private static final Map<Class<? extends Annotation>, ConstraintMapper> MAPPERS = Map.ofEntries(
        Map.entry(NotNull.class, ann -> Map.of("required", true)),
        Map.entry(NotBlank.class, ann -> Map.of("required", true, "minLength", 1)),
        Map.entry(NotEmpty.class, ann -> Map.of("required", true, "minLength", 1)),
        Map.entry(Size.class, ann -> {
            Size s = (Size) ann;
            Map<String, Object> m = new HashMap<>();
            if (s.min() >= 0) m.put("minLength", s.min());
            if (s.max() < Integer.MAX_VALUE) m.put("maxLength", s.max());
            return m;
        }),
        Map.entry(Min.class, ann -> Map.of("minimum", (long) ((Min) ann).value())),
        Map.entry(Max.class, ann -> Map.of("maximum", (long) ((Max) ann).value())),
        Map.entry(DecimalMin.class, ann -> {
            DecimalMin d = (DecimalMin) ann;
            Map<String, Object> m = new HashMap<>();
            m.put("minimum", Double.parseDouble(d.value()));
            m.put("exclusiveMinimum", !d.inclusive());
            return m;
        }),
        Map.entry(DecimalMax.class, ann -> {
            DecimalMax d = (DecimalMax) ann;
            Map<String, Object> m = new HashMap<>();
            m.put("maximum", Double.parseDouble(d.value()));
            m.put("exclusiveMaximum", !d.inclusive());
            return m;
        }),
        Map.entry(Pattern.class, ann -> Map.of("pattern", ((Pattern) ann).regexp())),
        Map.entry(Positive.class, ann -> Map.of("minimum", 0, "exclusiveMinimum", true)),
        Map.entry(PositiveOrZero.class, ann -> Map.of("minimum", 0)),
        Map.entry(Negative.class, ann -> Map.of("maximum", 0, "exclusiveMaximum", true)),
        Map.entry(NegativeOrZero.class, ann -> Map.of("maximum", 0)),
        Map.entry(Email.class, ann -> Map.of("format", "email"))
    );

    @FunctionalInterface
    interface ConstraintMapper {
        Map<String, Object> map(Annotation ann);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: OpenApiConstraintVerifier <service-url> [<service-url>...]");
            System.exit(1);
        }

        // Scan classpath for DTO classes
        List<Class<?>> dtoClasses = scanForDtoClasses();
        System.out.println("Found " + dtoClasses.size() + " DTO classes with validation annotations:");
        for (Class<?> dto : dtoClasses) {
            System.out.println("  - " + dto.getName());
        }

        List<String> allErrors = new ArrayList<>();

        for (String baseUrl : args) {
            String serviceName = extractServiceName(baseUrl);
            System.out.println("\n============================================================");
            System.out.println("Verifying " + serviceName + " at " + baseUrl);
            System.out.println("============================================================");

            JsonNode spec = fetchOpenApiSpec(baseUrl);
            if (spec == null) {
                allErrors.add("Failed to fetch OpenAPI spec from " + baseUrl);
                continue;
            }

            Map<String, JsonNode> schemas = extractSchemas(spec);
            System.out.println("Found " + schemas.size() + " schema components:");
            for (String name : schemas.keySet()) {
                System.out.println("  - " + name);
            }

            for (Class<?> dtoClass : dtoClasses) {
                JsonNode schema = findMatchingSchema(dtoClass, schemas);
                if (schema == null) {
                    // Not necessarily an error - some DTOs might not be exposed
                    System.out.println("  ⚠ No matching schema for " + dtoClass.getSimpleName());
                    continue;
                }

                System.out.println("  Checking " + dtoClass.getSimpleName() + " -> " + getSchemaName(schema, dtoClass.getSimpleName()));
                List<String> errors = verifyClass(dtoClass, schema);
                if (!errors.isEmpty()) {
                    for (String err : errors) {
                        allErrors.add(dtoClass.getSimpleName() + "." + err);
                    }
                }
            }
        }

        System.out.println("\n============================================================");
        System.out.println("SUMMARY");
        System.out.println("============================================================");

        if (!allErrors.isEmpty()) {
            System.err.println("FAILED: " + allErrors.size() + " constraint mismatch(es) found:");
            for (String err : allErrors) {
                System.err.println("  - " + err);
            }
            System.exit(1);
        } else {
            System.out.println("PASSED: All validation annotations have corresponding OpenAPI constraints");
            System.exit(0);
        }
    }

    private static List<Class<?>> scanForDtoClasses() {
        // In a real implementation, this would scan the classpath
        // For now, we'll use a known list of DTO packages
        String[] dtoPackages = {
            "com.example.common.dto",
            "com.example.product",
            "com.example.category",
            "com.example.order",
            "com.example.inventory",
            "com.example.notification",
            "com.example.payment"
        };

        List<Class<?>> dtos = new ArrayList<>();
        
        // Use a simple approach: try to load known DTO classes
        String[] knownDtos = {
            "com.example.common.dto.OrderDto",
            "com.example.common.dto.OrderItemDto",
            "com.example.common.dto.ProductDto",
            "com.example.common.dto.ProductVariantDto",
            "com.example.common.dto.AuthorizeRequest",
            "com.example.common.dto.CaptureRequest",
            "com.example.common.dto.RefundRequest",
            "com.example.common.dto.ReserveStockRequest",
            "com.example.common.dto.ConfirmStockRequest",
            "com.example.common.dto.InventoryDto",
            "com.example.common.dto.NotificationDto",
            "com.example.common.dto.ShipmentDto",
            "com.example.common.dto.ShipmentItemDto",
            "com.example.common.dto.PaymentDto",
            "com.example.common.dto.CategoryDto",
            "com.example.common.dto.PageResponse"
        };

        for (String className : knownDtos) {
            try {
                Class<?> clazz = Class.forName(className);
                if (hasValidationAnnotations(clazz)) {
                    dtos.add(clazz);
                }
            } catch (ClassNotFoundException e) {
                // Class not on classpath, skip
            }
        }

        return dtos;
    }

    private static boolean hasValidationAnnotations(Class<?> clazz) {
        // Check record components (Java 16+)
        if (clazz.isRecord()) {
            for (java.lang.reflect.RecordComponent component : clazz.getRecordComponents()) {
                for (Annotation ann : component.getAnnotations()) {
                    if (MAPPERS.containsKey(ann.annotationType())) {
                        System.out.println("    [DEBUG] Found validation annotation on record component: " + clazz.getSimpleName() + "." + component.getName() + " -> " + ann.annotationType().getSimpleName());
                        return true;
                    }
                }
            }
        }
        
        // Fallback for regular classes
        for (Field field : clazz.getDeclaredFields()) {
            for (Annotation ann : field.getAnnotations()) {
                if (MAPPERS.containsKey(ann.annotationType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static JsonNode fetchOpenApiSpec(String baseUrl) {
        String url = baseUrl.replaceAll("/$", "") + "/v3/api-docs";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(java.time.Duration.ofSeconds(30))
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return MAPPER.readTree(response.body());
            } else {
                System.err.println("HTTP " + response.statusCode() + " from " + url);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error fetching " + url + ": " + e.getMessage());
        }
        return null;
    }

    private static Map<String, JsonNode> extractSchemas(JsonNode spec) {
        Map<String, JsonNode> schemas = new HashMap<>();
        JsonNode components = spec.get("components");
        if (components != null) {
            JsonNode schemasNode = components.get("schemas");
            if (schemasNode != null && schemasNode.isObject()) {
                schemasNode.fields().forEachRemaining(entry -> schemas.put(entry.getKey(), entry.getValue()));
            }
        }
        return schemas;
    }

    private static JsonNode findMatchingSchema(Class<?> dtoClass, Map<String, JsonNode> schemas) {
        String simpleName = dtoClass.getSimpleName();
        
        // Try exact match
        if (schemas.containsKey(simpleName)) {
            return schemas.get(simpleName);
        }

        // Try with package prefix
        String fullName = dtoClass.getName().replace(".", "");
        for (String schemaName : schemas.keySet()) {
            if (schemaName.endsWith(simpleName) || schemaName.contains(simpleName)) {
                return schemas.get(schemaName);
            }
        }

        return null;
    }

    private static String getSchemaName(JsonNode schema, String fallback) {
        return schema.get("title") != null ? schema.get("title").asText() : fallback;
    }

    private static List<String> verifyClass(Class<?> dtoClass, JsonNode schema) {
        List<String> errors = new ArrayList<>();
        JsonNode properties = schema.get("properties");
        JsonNode required = schema.get("required");

        Set<String> requiredFields = new HashSet<>();
        if (required != null && required.isArray()) {
            for (JsonNode r : required) {
                requiredFields.add(r.asText());
            }
        }

        // Check record components (Java 16+)
        if (dtoClass.isRecord()) {
            for (java.lang.reflect.RecordComponent component : dtoClass.getRecordComponents()) {
                String fieldName = component.getName();
                JsonNode prop = properties != null ? properties.get(fieldName) : null;
                if (prop == null) continue;

                errors.addAll(verifyRecordComponent(fieldName, component, prop, requiredFields));
            }
        } else {
            // Fallback for regular classes
            for (Field field : dtoClass.getDeclaredFields()) {
                String fieldName = field.getName();
                JsonNode prop = properties != null ? properties.get(fieldName) : null;
                if (prop == null) continue;

                errors.addAll(verifyField(fieldName, field, prop, requiredFields));
            }
        }

        return errors;
    }

    private static List<String> verifyField(String fieldName, Field field, JsonNode prop, Set<String> requiredFields) {
        List<String> errors = new ArrayList<>();
        boolean hasNotNull = false;

        for (Annotation ann : field.getAnnotations()) {
            ConstraintMapper mapper = MAPPERS.get(ann.annotationType());
            if (mapper == null) continue;

            hasNotNull |= ann instanceof NotNull || ann instanceof NotBlank || ann instanceof NotEmpty;
            Map<String, Object> expected = mapper.map(ann);

            for (Map.Entry<String, Object> entry : expected.entrySet()) {
                String key = entry.getKey();
                Object expectedValue = entry.getValue();
                
                if (expectedValue == null) continue;
                
                JsonNode actualNode = prop.get(key);
                if (actualNode == null || actualNode.isNull()) {
                    errors.add(fieldName + ": Missing OpenAPI constraint '" + key + "' for @" + ann.annotationType().getSimpleName() + " (expected: " + expectedValue + ")");
                } else {
                    Object actualValue = convertJsonNode(actualNode);
                    if (!valuesMatch(expectedValue, actualValue)) {
                        errors.add(fieldName + ": OpenAPI constraint '" + key + "' mismatch for @" + ann.annotationType().getSimpleName() + ". Expected " + expectedValue + ", got " + actualValue);
                    }
                }
            }
        }

        // Check required
        boolean isRequired = requiredFields.contains(fieldName);
        if (hasNotNull && !isRequired) {
            errors.add(fieldName + ": Has @NotNull/@NotBlank but not marked required in schema");
        } else if (!hasNotNull && isRequired) {
            errors.add(fieldName + ": Marked required in schema but lacks @NotNull/@NotBlank");
        }

        return errors;
    }

    private static List<String> verifyRecordComponent(String fieldName, java.lang.reflect.RecordComponent component, JsonNode prop, Set<String> requiredFields) {
        List<String> errors = new ArrayList<>();
        boolean hasNotNull = false;

        for (Annotation ann : component.getAnnotations()) {
            ConstraintMapper mapper = MAPPERS.get(ann.annotationType());
            if (mapper == null) continue;

            hasNotNull |= ann instanceof NotNull || ann instanceof NotBlank || ann instanceof NotEmpty;
            Map<String, Object> expected = mapper.map(ann);

            for (Map.Entry<String, Object> entry : expected.entrySet()) {
                String key = entry.getKey();
                Object expectedValue = entry.getValue();
                
                if (expectedValue == null) continue;
                
                JsonNode actualNode = prop.get(key);
                if (actualNode == null || actualNode.isNull()) {
                    errors.add(fieldName + ": Missing OpenAPI constraint '" + key + "' for @" + ann.annotationType().getSimpleName() + " (expected: " + expectedValue + ")");
                } else {
                    Object actualValue = convertJsonNode(actualNode);
                    if (!valuesMatch(expectedValue, actualValue)) {
                        errors.add(fieldName + ": OpenAPI constraint '" + key + "' mismatch for @" + ann.annotationType().getSimpleName() + ". Expected " + expectedValue + ", got " + actualValue);
                    }
                }
            }
        }

        // Check required
        boolean isRequired = requiredFields.contains(fieldName);
        if (hasNotNull && !isRequired) {
            errors.add(fieldName + ": Has @NotNull/@NotBlank but not marked required in schema");
        } else if (!hasNotNull && isRequired) {
            errors.add(fieldName + ": Marked required in schema but lacks @NotNull/@NotBlank");
        }

        return errors;
    }

    private static Object convertJsonNode(JsonNode node) {
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        return node.asText();
    }

    private static boolean valuesMatch(Object expected, Object actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        
        if (expected instanceof Number && actual instanceof Number) {
            double exp = ((Number) expected).doubleValue();
            double act = ((Number) actual).doubleValue();
            return Math.abs(exp - act) < 0.001;
        }
        
        return expected.equals(actual);
    }

    private static String extractServiceName(String url) {
        if (url.contains(":8081")) return "product";
        if (url.contains(":8082")) return "category";
        if (url.contains(":8083")) return "order-service";
        if (url.contains(":8084")) return "inventory-service";
        if (url.contains(":8086")) return "payment-service";
        if (url.contains(":8087")) return "notification-service";
        if (url.contains(":8080")) return "gateway";
        return "unknown";
    }
}