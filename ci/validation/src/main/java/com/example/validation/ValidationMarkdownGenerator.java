package com.example.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates VALIDATION.md from Jakarta Validation annotations on DTO fields.
 * Scans all modules for DTO classes and produces per-module Markdown tables.
 */
public class ValidationMarkdownGenerator {

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
            System.err.println("Usage: ValidationMarkdownGenerator <output-path> [<service-url>...]");
            System.exit(1);
        }

        String outputPath = args[0];
        List<String> serviceUrls = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            serviceUrls.add(args[i]);
        }

        // Scan classpath for DTO classes
        List<Class<?>> dtoClasses = scanForDtoClasses();
        System.out.println("Found " + dtoClasses.size() + " DTO classes with validation annotations");

        // Group DTOs by module
        Map<String, List<Class<?>>> dtosByModule = groupByModule(dtoClasses);

        // Fetch OpenAPI specs for schema paths if URLs provided
        Map<String, JsonNode> allSchemas = new HashMap<>();
        if (!serviceUrls.isEmpty()) {
            for (String baseUrl : serviceUrls) {
                String serviceName = extractServiceName(baseUrl);
                System.out.println("Fetching OpenAPI spec from " + serviceName + " at " + baseUrl);
                JsonNode spec = fetchOpenApiSpec(baseUrl);
                if (spec != null) {
                    Map<String, JsonNode> schemas = extractSchemas(spec);
                    for (Map.Entry<String, JsonNode> entry : schemas.entrySet()) {
                        allSchemas.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        // Generate Markdown
        String markdown = generateMarkdown(dtosByModule, allSchemas);

        // Write to file
        Path outputFile = Paths.get(outputPath);
        Files.writeString(outputFile, markdown);
        System.out.println("Generated " + outputFile.toAbsolutePath());
    }

    private static Map<String, List<Class<?>>> groupByModule(List<Class<?>> dtoClasses) {
        Map<String, List<Class<?>>> grouped = new LinkedHashMap<>();
        
        // Define module mapping based on package names
        Map<String, String> packageToModule = Map.ofEntries(
            Map.entry("com.example.common.dto", "common"),
            Map.entry("com.example.product", "product-service"),
            Map.entry("com.example.category", "category-service"),
            Map.entry("com.example.order", "order-service"),
            Map.entry("com.example.inventory", "inventory-service"),
            Map.entry("com.example.notification", "notification-service"),
            Map.entry("com.example.payment", "payment-service")
        );

        for (Class<?> dto : dtoClasses) {
            String module = "unknown";
            for (Map.Entry<String, String> entry : packageToModule.entrySet()) {
                if (dto.getName().startsWith(entry.getKey())) {
                    module = entry.getValue();
                    break;
                }
            }
            grouped.computeIfAbsent(module, k -> new ArrayList<>()).add(dto);
        }

        return grouped;
    }

    private static String generateMarkdown(Map<String, List<Class<?>>> dtosByModule, Map<String, JsonNode> schemas) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# Validation Rules Reference\n\n");
        sb.append("*Auto-generated from Jakarta Validation annotations on DTO fields.*\n\n");
        sb.append("---\n\n");

        for (Map.Entry<String, List<Class<?>>> entry : dtosByModule.entrySet()) {
            String module = entry.getKey();
            List<Class<?>> dtos = entry.getValue();

            sb.append("## ").append(module).append("\n\n");

            for (Class<?> dtoClass : dtos) {
                sb.append("### ").append(dtoClass.getSimpleName()).append("\n\n");
                
                List<ValidationRow> rows = collectValidationRows(dtoClass, schemas);
                
                if (rows.isEmpty()) {
                    sb.append("*No validation annotations found.*\n\n");
                    continue;
                }

                // Table header
                sb.append("| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |\n");
                sb.append("|-------|------------|------------|-------------|---------------|-----------------|\n");

                for (ValidationRow row : rows) {
                    sb.append("| ")
                      .append(escape(row.fieldName)).append(" | ")
                      .append(escape(row.annotation)).append(" | ")
                      .append(escape(row.parameters)).append(" | ")
                      .append(escape(row.schemaPath)).append(" | ")
                      .append(escape(row.validExample)).append(" | ")
                      .append(escape(row.invalidExample)).append(" |\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static List<ValidationRow> collectValidationRows(Class<?> dtoClass, Map<String, JsonNode> schemas) {
        List<ValidationRow> rows = new ArrayList<>();
        JsonNode schema = findMatchingSchema(dtoClass, schemas);

        if (dtoClass.isRecord()) {
            for (RecordComponent component : dtoClass.getRecordComponents()) {
                rows.addAll(collectFromRecordComponent(component, schema));
            }
        } else {
            for (Field field : dtoClass.getDeclaredFields()) {
                rows.addAll(collectFromField(field, schema));
            }
        }

        return rows;
    }

    private static List<ValidationRow> collectFromRecordComponent(RecordComponent component, JsonNode schema) {
        List<ValidationRow> rows = new ArrayList<>();
        String fieldName = component.getName();
        Method accessor = component.getAccessor();

        for (Annotation ann : accessor.getAnnotations()) {
            ConstraintMapper mapper = MAPPERS.get(ann.annotationType());
            if (mapper == null) continue;

            Map<String, Object> expected = mapper.map(ann);
            String annotationName = ann.annotationType().getSimpleName();
            String parameters = formatParameters(ann);
            String schemaPath = findSchemaPath(schema, fieldName);
            String[] examples = generateExamples(ann, expected);

            rows.add(new ValidationRow(fieldName, "@" + annotationName, parameters, schemaPath, examples[0], examples[1]));
        }

        return rows;
    }

    private static List<ValidationRow> collectFromField(Field field, JsonNode schema) {
        List<ValidationRow> rows = new ArrayList<>();
        String fieldName = field.getName();

        for (Annotation ann : field.getAnnotations()) {
            ConstraintMapper mapper = MAPPERS.get(ann.annotationType());
            if (mapper == null) continue;

            Map<String, Object> expected = mapper.map(ann);
            String annotationName = ann.annotationType().getSimpleName();
            String parameters = formatParameters(ann);
            String schemaPath = findSchemaPath(schema, fieldName);
            String[] examples = generateExamples(ann, expected);

            rows.add(new ValidationRow(fieldName, "@" + annotationName, parameters, schemaPath, examples[0], examples[1]));
        }

        return rows;
    }

    private static String formatParameters(Annotation ann) {
        if (ann instanceof Size) {
            Size s = (Size) ann;
            StringBuilder sb = new StringBuilder();
            if (s.min() > 0) {
                sb.append("min=").append(s.min());
            }
            if (s.max() < Integer.MAX_VALUE) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("max=").append(s.max());
            }
            String msg = s.message();
            if (msg != null && !msg.isEmpty() && !msg.equals("{jakarta.validation.constraints.Size.message}")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("message=\"").append(msg).append("\"");
            }
            return sb.toString();
        }
        if (ann instanceof Min) {
            return "value=" + ((Min) ann).value();
        }
        if (ann instanceof Max) {
            return "value=" + ((Max) ann).value();
        }
        if (ann instanceof DecimalMin) {
            DecimalMin d = (DecimalMin) ann;
            return "value=" + d.value() + ", inclusive=" + d.inclusive();
        }
        if (ann instanceof DecimalMax) {
            DecimalMax d = (DecimalMax) ann;
            return "value=" + d.value() + ", inclusive=" + d.inclusive();
        }
        if (ann instanceof Pattern) {
            return "regexp=\"" + ((Pattern) ann).regexp() + "\"";
        }
        if (ann instanceof NotBlank) {
            NotBlank nb = (NotBlank) ann;
            return nb.message() != null && !nb.message().isEmpty() ? "message=\"" + nb.message() + "\"" : "";
        }
        if (ann instanceof NotNull) {
            NotNull nn = (NotNull) ann;
            return nn.message() != null && !nn.message().isEmpty() ? "message=\"" + nn.message() + "\"" : "";
        }
        if (ann instanceof NotEmpty) {
            NotEmpty ne = (NotEmpty) ann;
            return ne.message() != null && !ne.message().isEmpty() ? "message=\"" + ne.message() + "\"" : "";
        }
        if (ann instanceof Email) {
            Email e = (Email) ann;
            return e.message() != null && !e.message().isEmpty() ? "message=\"" + e.message() + "\"" : "";
        }
        return "";
    }

    private static String[] generateExamples(Annotation ann, Map<String, Object> expected) {
        String valid = "";
        String invalid = "";

        if (ann instanceof NotBlank) {
            valid = "\"example\"";
            invalid = "\"\"";
        } else if (ann instanceof NotNull) {
            valid = "\"example\"";
            invalid = "null";
        } else if (ann instanceof NotEmpty) {
            valid = "\"example\"";
            invalid = "\"\"";
        } else if (ann instanceof Size) {
            Size s = (Size) ann;
            int max = s.max() < Integer.MAX_VALUE ? s.max() : 255;
            int min = s.min() >= 0 ? s.min() : 1;
            if (min > 0 && max >= min) {
                valid = "\"x\".repeat(" + min + ")";
                invalid = "\"x\".repeat(" + (max + 1) + ")";
            } else if (max < Integer.MAX_VALUE) {
                valid = "\"short\"";
                invalid = "\"x\".repeat(" + (max + 1) + ")";
            } else {
                valid = "\"example\"";
                invalid = "\"x\".repeat(1000)";
            }
        } else if (ann instanceof Min) {
            long min = ((Min) ann).value();
            valid = String.valueOf(min);
            invalid = String.valueOf(min - 1);
        } else if (ann instanceof Max) {
            long max = ((Max) ann).value();
            valid = String.valueOf(max);
            invalid = String.valueOf(max + 1);
        } else if (ann instanceof DecimalMin) {
            DecimalMin d = (DecimalMin) ann;
            double min = Double.parseDouble(d.value());
            valid = String.valueOf(d.inclusive() ? min : min + 0.1);
            invalid = String.valueOf(d.inclusive() ? min - 0.1 : min);
        } else if (ann instanceof DecimalMax) {
            DecimalMax d = (DecimalMax) ann;
            double max = Double.parseDouble(d.value());
            valid = String.valueOf(d.inclusive() ? max : max - 0.1);
            invalid = String.valueOf(d.inclusive() ? max + 0.1 : max);
        } else if (ann instanceof Pattern) {
            valid = "\"matching-pattern\"";
            invalid = "\"non-matching\"";
        } else if (ann instanceof Email) {
            valid = "\"user@example.com\"";
            invalid = "\"invalid-email\"";
        } else if (ann instanceof Positive) {
            valid = "1";
            invalid = "0";
        } else if (ann instanceof PositiveOrZero) {
            valid = "0";
            invalid = "-1";
        } else if (ann instanceof Negative) {
            valid = "-1";
            invalid = "0";
        } else if (ann instanceof NegativeOrZero) {
            valid = "0";
            invalid = "1";
        }

        return new String[]{valid, invalid};
    }

    private static String findSchemaPath(JsonNode schema, String fieldName) {
        if (schema == null) return "N/A";
        String title = schema.get("title") != null ? schema.get("title").asText() : "UnknownSchema";
        JsonNode properties = schema.get("properties");
        if (properties != null && properties.has(fieldName)) {
            return title + "." + fieldName;
        }
        return "N/A";
    }

    private static JsonNode findMatchingSchema(Class<?> dtoClass, Map<String, JsonNode> schemas) {
        String simpleName = dtoClass.getSimpleName();

        if (schemas.containsKey(simpleName)) {
            return schemas.get(simpleName);
        }

        for (String schemaName : schemas.keySet()) {
            if (schemaName.endsWith(simpleName) || schemaName.contains(simpleName)) {
                return schemas.get(schemaName);
            }
        }

        return null;
    }

    private static List<Class<?>> scanForDtoClasses() {
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

        List<Class<?>> dtos = new ArrayList<>();
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
        if (clazz.isRecord()) {
            for (RecordComponent component : clazz.getRecordComponents()) {
                Method accessor = component.getAccessor();
                for (Annotation ann : accessor.getAnnotations()) {
                    if (MAPPERS.containsKey(ann.annotationType())) {
                        return true;
                    }
                }
            }
        }
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

    private static String extractServiceName(String url) {
        if (url.contains(":8081")) return "product";
        if (url.contains(":8082")) return "category";
        if (url.contains(":8083")) return "order-service";
        if (url.contains(":8084")) return "inventory-service";
        if (url.contains(":8085")) return "notification-service";
        if (url.contains(":8086")) return "payment-service";
        if (url.contains(":8080")) return "gateway";
        return "unknown";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }

    private static class ValidationRow {
        final String fieldName;
        final String annotation;
        final String parameters;
        final String schemaPath;
        final String validExample;
        final String invalidExample;

        ValidationRow(String fieldName, String annotation, String parameters, String schemaPath, String validExample, String invalidExample) {
            this.fieldName = fieldName;
            this.annotation = annotation;
            this.parameters = parameters;
            this.schemaPath = schemaPath;
            this.validExample = validExample;
            this.invalidExample = invalidExample;
        }
    }
}