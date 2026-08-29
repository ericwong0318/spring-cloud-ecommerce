# TODO

## Build & Test Issues
- [x] Fix Testcontainers version - BOM 2.0.5 not resolving correct version (still getting 1.19.8) - Using 1.21.4 directly
- [x] Fix Docker connection for Testcontainers with OrbStack - Working with Docker API 1.54
- [x] Run full test suite (mvn test) - all modules pass
- [x] Run integration tests (mvn verify -Dskip.unit.tests=true)
- [x] Code coverage report (mvn jacoco:report)

## System Test Module
- [x] system-test depends on docker-java 3.3.6 which doesn't support Docker API 1.40+ - Testcontainers 1.21.4 uses docker-java 3.4.2
- [x] Need docker-java 3.4.x or 4.x for OrbStack/Docker 29.x compatibility - Already satisfied

## Other
- [x] Verify auth-server warning about deprecated applyDefaultSecurity - No warning found
- [x] Check order-service, inventory-service, notification-service have no tests - CONFIRMED: No tests exist
- [x] Add unit tests for order-service
- [x] Add unit tests for inventory-service
- [x] Add unit tests for notification-service
- [x] Add unit tests for auth-server

## Documentation
- [x] Update README with test running instructions
- [x] Document Docker/OrbStack setup for Testcontainers
- [x] Document service startup order