# TODO

## Build & Test Issues
- [ ] Fix Testcontainers version - BOM 2.0.5 not resolving correct version (still getting 1.19.8)
- [ ] Fix Docker connection for Testcontainers with OrbStack
- [ ] Run full test suite (mvn test) - all modules pass
- [ ] Run integration tests (mvn verify -Dskip.unit.tests=true)
- [ ] Code coverage report (mvn jacoco:report)

## System Test Module
- [ ] system-test depends on docker-java 3.3.6 which doesn't support Docker API 1.40+
- [ ] Need docker-java 3.4.x or 4.x for OrbStack/Docker 29.x compatibility

## Other
- [ ] Verify auth-server warning about deprecated applyDefaultSecurity
- [ ] Check order-service, inventory-service, notification-service have no tests