package com.example.category.pact;

import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.core.model.PactSource;
import au.com.dius.pact.provider.junitsupport.loader.PactFolderLoader;
import au.com.dius.pact.provider.junitsupport.loader.PactLoader;
import au.com.dius.pact.provider.junit5.PactVerificationExtension;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.core.support.expressions.ValueResolver;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

public class CategoryPactExtension extends PactVerificationInvocationContextProvider {

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        PactFolderLoader loader = new PactFolderLoader(new File("../product/target/pacts"));
        List<Pact> pacts = loader.load("category-service");

        return pacts.stream()
            .flatMap(pact -> pact.getInteractions().stream()
                .map(interaction -> new PactVerificationExtension(
                    pact,
                    pact.getSource(),
                    interaction,
                    "category-service",
                    "product-service",
                    new ValueResolver() {
                        @Override
                        public String resolveValue(String expression) {
                            return System.getProperty(expression);
                        }

                        @Override
                        public String resolveValue(String expression, String defaultValue) {
                            String value = System.getProperty(expression);
                            return value != null ? value : defaultValue;
                        }

                        @Override
                        public boolean propertyDefined(String expression) {
                            return System.getProperty(expression) != null;
                        }
                    }
                ))
            );
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }
}