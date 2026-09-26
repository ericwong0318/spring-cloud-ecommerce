# ADR 013: CI Pipeline Fixes (spring-boot-maven-plugin, OWASP JAVA_HOME)

## Status
Accepted

## Context
The CI pipeline had two critical issues:
1. **spring-boot-maven-plugin** not repackaging JARs - declared in parent POM `<plugins>` with hardcoded `mainClass`, incorrectly inherited by all modules
2. **OWASP Dependency Check** failing with "JAVA_HOME not defined correctly" - hardcoded `/opt/jdk` path didn't match GitHub Actions runner's actual Java path

## Decision
1. **Move spring-boot-maven-plugin to pluginManagement** in parent POM, declare explicitly in each service's `pom.xml` with correct `mainClass`
2. **Remove hardcoded JAVA_HOME** from security-scan job, let action auto-detect after setup-java

## Fix 1: spring-boot-maven-plugin

### Before (Broken)
```xml
<!-- parent pom.xml <plugins> - WRONG -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <mainClass>org.example.configserver.ConfigServerApplication</mainClass>
    </configuration>
</plugin>
```
Problems: 
- Inherited by ALL modules with Config Server's mainClass
- No explicit `<execution>` binding for `repackage` goal
- Maven 3.x doesn't auto-bind from metadata

### After (Fixed)
```xml
<!-- parent pom.xml <pluginManagement> -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <version>${spring-boot.version}</version>
    <executions>
        <execution>
            <id>repackage</id>
            <goals>
                <goal>repackage</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!-- Each service's pom.xml -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <mainClass>org.example.payment.PaymentServiceApplication</mainClass>
    </configuration>
</plugin>
```

## Fix 2: OWASP Dependency Check JAVA_HOME

### Before (Broken)
```yaml
# .github/workflows/ci.yml
- name: Run OWASP Dependency Check
  uses: dependency-check/Dependency-Check_Action@main
  env:
    JAVA_HOME: /opt/jdk  # HARDCODED - WRONG PATH
```

### After (Fixed)
```yaml
# .github/workflows/ci.yml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: maven

- name: Run OWASP Dependency Check
  uses: dependency-check/Dependency-Check_Action@main
  # NO JAVA_HOME - auto-detected from PATH after setup-java
  with:
    project: 'payment-service'
    path: 'payment-service'
    format: 'HTML'
    out: 'reports'
    args: '--failOnCVSS 7'
```

## Consequences

### Positive
- All 11 service JARs properly repackaged with Spring Boot manifest
- Security scan runs without JAVA_HOME errors
- Each service declares its own mainClass (no inheritance conflicts)
- Explicit repackage execution binding works with Maven 3.x

### Negative
- More verbose: each service needs spring-boot-maven-plugin declaration
- Parent POM pluginManagement section grows

## Verification
```bash
# Verify JAR repackaging
mvn clean package -pl config-server -DskipTests
unzip -l config-server/target/config-server-*.jar | grep -E "(Main-Class|Start-Class|BOOT-INF)"

# Expected output:
# Main-Class: org.springframework.boot.loader.launch.JarLauncher
# Start-Class: org.example.configserver.ConfigServerApplication
# BOOT-INF/classes/
# BOOT-INF/lib/
```

## Related
- `.scratch/ci-pipeline-fix/issues/01-debug-spring-boot-maven-plugin.md`
- `.scratch/ci-pipeline-fix/issues/06-fix-security-scan-java-home.md`
- `.github/workflows/ci.yml`
- Parent `pom.xml` pluginManagement section
