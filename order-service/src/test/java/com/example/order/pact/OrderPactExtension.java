package com.example.order.pact;

import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.provider.junitsupport.loader.PactFolderLoader;
import au.com.dius.pact.provider.junit5.PactVerificationExtension;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.core.support.expressions.ValueResolver;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class OrderPactExtension extends PactVerificationInvocationContextProvider {

    private static final Map<String, String> CONSUMER_TO_MODULE = Map.of(
        "product-service", "product",
        "payment-service", "payment-service",
        "inventory-service", "inventory-service"
    );

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        List<TestTemplateInvocationContext> contexts = new ArrayList<>();

        // Get current working directory (module directory in Maven reactor)
        File cwd = new File(System.getProperty("user.dir")).getAbsoluteFile();
        
        // Try to detect project root (parent of module directory)
        File projectRoot = cwd.getParentFile();

        for (Map.Entry<String, String> entry : CONSUMER_TO_MODULE.entrySet()) {
            String consumer = entry.getKey();
            String module = entry.getValue();

            File pactDir = resolvePactDirectory(cwd, projectRoot, module);
            if (!pactDir.exists() || !pactDir.isDirectory()) {
                continue;
            }

            PactFolderLoader loader = new PactFolderLoader(pactDir);
            List<Pact> pacts = loader.load("order-service");

            for (Pact pact : pacts) {
                for (var interaction : pact.getInteractions()) {
                    contexts.add(new PactVerificationExtension(
                        pact,
                        pact.getSource(),
                        interaction,
                        "order-service",
                        consumer,
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
                    ));
                }
            }
        }

        return contexts.stream();
    }

    private File resolvePactDirectory(File cwd, File projectRoot, String module) {
        // Try 1: Relative to current working directory (module directory in reactor)
        // For order-service module: ../product/target/pacts, ../payment-service/target/pacts, etc.
        File relativeToCwd = new File(cwd, "../" + module + "/target/pacts");
        if (relativeToCwd.exists()) {
            return relativeToCwd;
        }

        // Try 2: Relative to project root (if running from root or different setup)
        // For order-service: order-service/target/pacts (but this is for consumer pacts, not provider)
        // For other modules: module/target/pacts
        if (projectRoot != null) {
            File relativeToRoot = new File(projectRoot, module + "/target/pacts");
            if (relativeToRoot.exists()) {
                return relativeToRoot;
            }
        }

        // Try 3: For order-service's own pacts (if order-service is a consumer of itself)
        // This would be target/pacts from order-service module directory
        if ("order-service".equals(module)) {
            File ownPacts = new File(cwd, "target/pacts");
            if (ownPacts.exists()) {
                return ownPacts;
            }
        }

        return relativeToCwd;
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }
}