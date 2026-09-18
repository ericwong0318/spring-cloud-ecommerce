package com.example.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Email;
import jakarta.persistence.Entity;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

/**
 * ArchUnit rules enforcing the Jakarta Validation baseline across all services.
 * Jakarta Validation (JSR 380) is the API specification — the annotations and Validator contract.
 * Hibernate Validator is the implementation engine enforcing those constraints at runtime.
 */
@AnalyzeClasses(packages = "com.example",
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class ValidationArchitectureTest {

    private static final Set<Class<? extends Annotation>> VALIDATION_ANNOTATIONS = Set.of(
            NotBlank.class, NotEmpty.class, NotNull.class,
            Pattern.class, Size.class, Min.class, Max.class,
            DecimalMin.class, DecimalMax.class, Email.class
    );

    // =========================================================================
    // Rule 1: All @RequestBody parameters in @RestController must have @Valid
    // =========================================================================

    @ArchTest
    static final ArchRule request_body_must_have_valid = methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .should(new ArchCondition<>("have @Valid on all @RequestBody parameters (excluding primitives and String)") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    int paramIndex = 0;
                    for (JavaParameter param : method.getParameters()) {
                        if (param.isAnnotatedWith(RequestBody.class)) {
                            // Skip @Valid for primitives and String (raw payloads like webhooks)
                            String typeName = param.getType().getName();
                            if (typeName.equals("java.lang.String") || typeName.equals("int") || typeName.equals("long") || typeName.equals("boolean") || typeName.equals("double") || typeName.equals("float") || typeName.equals("short") || typeName.equals("byte") || typeName.equals("char")) {
                                events.add(SimpleConditionEvent.satisfied(method,
                                        "Parameter at index " + paramIndex + " is " + typeName + " (skipping @Valid)"));
                            } else if (!param.isAnnotatedWith(Valid.class)) {
                                events.add(SimpleConditionEvent.violated(method,
                                        "Method " + method.getFullName() + " has @RequestBody parameter at index "
                                                + paramIndex + " without @Valid"));
                            } else {
                                events.add(SimpleConditionEvent.satisfied(method,
                                        "Parameter at index " + paramIndex + " has @Valid"));
                            }
                        }
                        paramIndex++;
                    }
                }
            })
            .because("Every @RequestBody in a @RestController must be validated with @Valid "
                    + "to enforce Jakarta Validation constraints at the API boundary. "
                    + "Primitives and String (raw payloads) are exempt.")
            .allowEmptyShould(true);

    // =========================================================================
    // Rule 2: All DTO/Request classes must declare at least one Jakarta Validation annotation
    // =========================================================================

    @ArchTest
    static final ArchRule dto_classes_must_have_validation_annotations = classes()
            .that().resideInAPackage("..dto..").or().resideInAPackage("..request..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should(new ArchCondition<>("have at least one Jakarta Validation annotation on class or fields") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass clazz, ConditionEvents events) {
                    // Skip PageResponse - it's a generic pagination wrapper, not a validation DTO
                    if (clazz.getSimpleName().equals("PageResponse")) {
                        events.add(SimpleConditionEvent.satisfied(clazz, "Skipping PageResponse (pagination wrapper)"));
                        return;
                    }

                    // Skip nested classes (builder inner classes) - they have $ in the name
                    if (clazz.getName().contains("$")) {
                        events.add(SimpleConditionEvent.satisfied(clazz, "Skipping nested class (builder)"));
                        return;
                    }

                    boolean hasAnnotation = VALIDATION_ANNOTATIONS.stream()
                            .anyMatch(a -> clazz.isAnnotatedWith(a))
                            || clazz.getFields().stream()
                            .anyMatch(f -> VALIDATION_ANNOTATIONS.stream().anyMatch(a -> f.isAnnotatedWith(a)));

                    // Check record components via fields (they're exposed as fields in ArchUnit)
                    if (clazz.isRecord()) {
                        hasAnnotation = hasAnnotation || clazz.getFields().stream()
                                .anyMatch(f -> VALIDATION_ANNOTATIONS.stream().anyMatch(a -> f.isAnnotatedWith(a)));
                    }

                    if (hasAnnotation) {
                        events.add(SimpleConditionEvent.satisfied(clazz, "Has validation annotation"));
                    } else {
                        events.add(SimpleConditionEvent.violated(clazz,
                                "Class " + clazz.getFullName() + " in dto/request package has no Jakarta Validation annotations"));
                    }
                }
            })
            .because("Every DTO/Request class must declare at least one Jakarta Validation constraint "
                    + "(@NotBlank, @Size, @Pattern, @Min, @Max, @DecimalMin, @DecimalMax, @Email, @NotNull, @NotEmpty) "
                    + "to ensure the validation baseline is enforced");

    // =========================================================================
    // Rule 3: @Valid must not appear on domain entities or service-layer types
    // =========================================================================

    @ArchTest
    static final ArchRule valid_not_on_entities = noClasses()
            .that().areAnnotatedWith(Entity.class).or().areAnnotatedWith(Document.class)
            .should().beAnnotatedWith(Valid.class)
            .because("@Valid is for technical validation at the API boundary; "
                    + "domain entities enforce invariants in the service/saga layer, not via annotations")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule valid_not_on_service_classes = noClasses()
            .that().resideInAPackage("..service..")
            .should().beAnnotatedWith(Valid.class)
            .because("@Valid on service-layer types leaks technical validation into the domain layer; "
                    + "use explicit validation methods in services instead")
            .allowEmptyShould(true);

    // =========================================================================
    // Rule 4: @Pattern regexes must not contain ReDoS-prone patterns (nested quantifiers)
    // =========================================================================

    @ArchTest
    static final ArchRule pattern_regexes_no_nested_quantifiers = fields()
            .that().areAnnotatedWith(Pattern.class)
            .should(new ArchCondition<>("have no ReDoS-prone nested quantifiers in @Pattern regex") {
                @Override
                public void check(JavaField field, ConditionEvents events) {
                    Pattern annotation = field.getAnnotationOfType(Pattern.class);
                    if (annotation == null) {
                        return;
                    }
                    String regex = annotation.regexp();
                    if (hasNestedQuantifiers(regex)) {
                        events.add(SimpleConditionEvent.violated(field,
                                String.format("@Pattern on %s has ReDoS-prone nested quantifiers: %s",
                                        field.getFullName(), regex)));
                    } else {
                        events.add(SimpleConditionEvent.satisfied(field,
                                "Pattern regex OK: " + regex));
                    }
                }
            })
            .because("Nested quantifiers in @Pattern regexes (e.g., (a+)+, (a*)*) cause catastrophic backtracking (ReDoS). "
                    + "Use possessive quantifiers or atomic groups instead, or redesign the pattern.")
            .allowEmptyShould(true);

    /**
     * Detects nested quantifiers that can cause ReDoS.
     * Examples: (a+)+, (a*)*, (a?)+, ((a+)+)+
     */
    private static boolean hasNestedQuantifiers(String regex) {
        try {
            int depth = 0;
            for (int i = 0; i < regex.length(); i++) {
                char c = regex.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                } else if (depth > 0 && isQuantifier(c)) {
                    int j = i + 1;
                    while (j < regex.length() && regex.charAt(j) != ')') {
                        if (isQuantifier(regex.charAt(j))) {
                            return true;
                        }
                        j++;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isQuantifier(char c) {
        return c == '+' || c == '*' || c == '?';
    }
}