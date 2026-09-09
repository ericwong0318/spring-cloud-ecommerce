# 01 — Config Server

**What to build:** Spring Cloud Config Server serving configuration from Git backend at `http://localhost:8888`. Services fetch config on startup via `bootstrap.yml` with `spring.cloud.config.uri`.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Config Server starts on port 8888
- [ ] Git backend configured (`file:///config` or remote repo)
- [ ] `/config/{application}/{profile}` endpoint returns YAML
- [ ] Actuator health endpoint at `/actuator/health`
- [ ] OpenAPI docs at `/swagger-ui.html`
- [ ] Unit tests for config loading
- [ ] Dockerfile + docker-compose entry