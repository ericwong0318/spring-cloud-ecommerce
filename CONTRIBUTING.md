# Contributing Guide

Thank you for contributing to the Spring Cloud Microservices Platform!

## How to contribute

1. **Fork the repo** and create a feature branch from `main`.
2. **Make your changes** following the coding standards below.
3. **Run the relevant tests** (`mvn test -pl <module>`).
4. **Open a pull request** against `main` with a clear description.
5. **Tick the checkboxes** in the ticket file (`.scratch/*/issues/*.md`) if your work resolves a tracked ticket.

## Coding standards

- **Java 21**, **Spring Boot 3.3.5**, **Spring Cloud 2023.0.4**
- Root POM manages all versions via `dependencyManagement`; each module has its own `pom.xml`
- **Lombok Removal Policy**: Lombok is being phased out. Do not add new Lombok annotations. When modifying a class that uses Lombok, convert `@Data`, `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Slf4j` to explicit code. Prefer Java Records for DTOs and explicit constructors for dependency injection. Remove the `lombok` dependency and annotation processor from the module's `pom.xml`.
- **Constructor injection** for all services
- Services return `Mono<ResponseEntity<>>` or `Flux<>` (WebFlux) or standard MVC return types (WebMVC)

## Commit messages

All commits must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. Authorized prefixes:

| Prefix | Use for |
|--------|---------|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `refactor:` | Code restructuring without behavior change |
| `test:` | Adding, updating, or repairing tests |
| `docs:` | Documentation changes |
| `chore:` | Maintenance, dependency updates, tooling config |
| `perf:` | Performance improvements |
| `style:` | Formatting, linting, cosmetic changes (no logic shifts) |

Format: `<prefix>: <short, lowercase description in imperative mood>`

Example: `feat: add product search endpoint`

## Git workflow

- **Always fetch before rebase**: `git fetch origin` before any `git rebase main`
- Use `git pull --rebase origin main` on feature branches
- Verify with `git status` and `git log --oneline -5` after rebase
- **Merge with `--ff-only`**: `git merge --ff-only feature-branch` (fails if not clean)

## Build & test commands

```bash
# Build all modules (from root)
mvn clean install -DskipTests

# Run unit tests for a specific module
mvn test -pl product

# Run integration tests (requires Docker/OrbStack for Testcontainers)
mvn verify -Dskip.unit.tests=true

# Run all unit tests
mvn test

# Code coverage report
mvn jacoco:report
```

### Module commands

```bash
mvn spring-boot:run -pl config-server      # port 8888
mvn spring-boot:run -pl eureka-server      # port 8761
mvn spring-boot:run -pl gateway            # port 8080
mvn spring-boot:run -pl product            # port 8081 (MongoDB)
mvn spring-boot:run -pl category           # port 8082
mvn spring-boot:run -pl auth-server        # port 9000
mvn spring-boot:run -pl order-service      # port 8083 (R2DBC)
mvn spring-boot:run -pl inventory-service  # port 8084
mvn spring-boot:run -pl payment-service    # port 8086 (R2DBC)
mvn spring-boot:run -pl notification-service # port 8087
```

### Startup order (local development without Docker)

Config Server → Eureka Server → Auth Server → Gateway → Business services

## Testing rules

- Follow the **AAA pattern**: Arrange, Act, Assert
- Use descriptive test names like `it('should return X when Y', ...)`
- Mock all external API calls and database connections
- Do not test private functions directly
- Unit: `*Test.java` (Surefire)
- Integration: `*IntegrationTest.java` (Failsafe + Testcontainers)

## Ticket tracking

Issues and specs live under `.scratch/`. After completing work on a ticket, tick the checkboxes in the ticket file (`.scratch/*/issues/*.md`) to mark tasks as done. See `docs/agents/issue-tracker.md`.

## Service ports (default)

| Service | Port |
|---------|------|
| config-server | 8888 |
| eureka-server | 8761 |
| gateway | 8080 |
| product | 8081 |
| category | 8082 |
| auth-server | 9000 |
| order-service | 8083 |
| inventory-service | 8084 |
| payment-service | 8086 |
| notification-service | 8087 |

## License

MIT
