package com.example.common.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

class ArchitectureTest {

    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter().importPackages("com.example.common");

    @Test
    void allDtoClassesShouldBeRecords() {
        ArchRule rule = classes()
            .that().resideInAPackage("..dto..")
            .and().areNotEnums()
            .should().beRecords()
            .because("DTOs should be immutable records for thread-safety and serialization; enums are allowed");
        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void noLombokAnnotationsInMainCode() {
        ArchRule rule = classes()
            .that().resideInAPackage("..")
            .should().notBeAnnotatedWith("lombok.*")
            .because("Lombok is being phased out; use explicit constructors, records, or builders");
        rule.check(IMPORTED_CLASSES);
    }
}