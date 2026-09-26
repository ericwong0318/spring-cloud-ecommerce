# ADR 012: Payment Service SecurityConfig Bean Naming Conflict Resolution

## Status
Accepted

## Context
Payment Service is **WebFlux** (reactive) while Common module provides **Servlet** (blocking) `SecurityConfig`. During component scanning, both `SecurityConfig` classes were detected, causing bean definition conflicts:

```
BeanDefinitionStoreException: 
  Bean definition with same name 'securityConfig' 
  already exists: 
  com.example.common.config.SecurityConfig
  com.example.payment.config.SecurityConfig
```

## Decision
**Rename Payment Service's SecurityConfig to PaymentSecurityConfig** with explicit profile and reactive configuration.

## Solution

### Common Module (Servlet - Blocking)
```java
// common/src/main/java/com/example/common/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
            .build();
    }
}
```

### Payment Service (WebFlux - Reactive)
```java
// payment-service/src/main/java/com/example/payment/config/PaymentSecurityConfig.java
@Configuration
@EnableWebFluxSecurity
@Profile("!test")
public class PaymentSecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyExchange().permitAll())  // Auth handled at Gateway
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
            .csrf(csrf -> csrf.disable())
            .build();
    }
}
```

### Payment Service Test Config
```java
// payment-service/src/test/java/com/example/payment/config/TestSecurityConfig.java
@Configuration
@Profile("test")
@EnableWebFluxSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    @Order(SecurityWebFiltersOrder.AUTHENTICATION - 1)
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(ex -> ex.anyExchange().permitAll())
            .csrf(csrf -> csrf.disable())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
            .build();
    }

    @Bean
    @Primary
    public ReactiveJwtDecoder jwtDecoder() {
        return new NimbusReactiveJwtDecoder(jwt -> {
            throw new BadJwtException("Test token validation disabled");
        });
    }
}
```

## Consequences

### Positive
- No bean naming conflicts during component scanning
- Clear separation: Servlet config in common, WebFlux config in service
- Test profile disables auth entirely for pact/provider tests
- Gateway handles OAuth2 validation (services trust Gateway)

### Negative
- Services must explicitly choose reactive vs servlet security
- Common module's SecurityConfig only usable by Servlet-based services

## Pattern for Future WebFlux Services
1. Create `*SecurityConfig` (e.g., `OrderSecurityConfig`)
2. Use `@EnableWebFluxSecurity` and `SecurityWebFilterChain`
3. Add `@Profile("!test")` to disable in tests
4. Create `TestSecurityConfig` with `@Profile("test")` and permit-all chain

## Related
- `.scratch/payment-service-config/issues/01-fix-securityconfig-conflict.md`
- `payment-service/src/main/java/com/example/payment/config/`
