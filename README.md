# moto-advisor

An intelligent motorcycle advisor application built with Spring Boot, Thymeleaf, JPA/SQLite, JADE agents, and OWL-based ontology reasoning.

## Project Status
> **Phase 0 — Scope Lock & Setup (Day 1)**
> MVP scope is frozen. See [MVP_SCOPE.md](MVP_SCOPE.md) before adding or changing any feature.

## MVP in One Line
A web form accepts rider preferences → advisor logic produces a ranked motorcycle recommendation → result is displayed in the browser.

## Tech Stack
| Layer | Technology |
|---|---|
| Web | Spring Boot 4 + Spring MVC + Thymeleaf |
| Persistence | Spring Data JPA + SQLite (via Hibernate Community Dialects) |
| Ontology | OWL API 5 |
| Multi-agent layer | JADE 4.6 |
| Build | Maven (Java 21) |

## Running Locally

Prerequisites: Java 21, Maven (or use the included `mvnw`).

### First-time setup — install JADE (one-time only)

JADE is not on Maven Central. Run this once to install it into your local Maven repository:

```bash
curl -o /tmp/jade-4.6.0.jar  https://jade.tilab.com/maven/com/tilab/jade/jade/4.6.0/jade-4.6.0.jar
curl -o /tmp/jade-4.6.0.pom  https://jade.tilab.com/maven/com/tilab/jade/jade/4.6.0/jade-4.6.0.pom
./mvnw install:install-file \
  -Dfile=/tmp/jade-4.6.0.jar \
  -DpomFile=/tmp/jade-4.6.0.pom \
  -DgroupId=com.tilab.jade -DartifactId=jade -Dversion=4.6.0 -Dpackaging=jar
```

### Start the application

```bash
./mvnw spring-boot:run
```

Open [http://localhost:19091](http://localhost:19091).

> Note: the app is configured to run on port `19091`, and JADE uses `19119` by default.

### What the app uses at runtime

- The user-facing web UI is served by Spring MVC + Thymeleaf.
- The active motorcycle catalog is stored in `src/main/resources/ontology/moto-advisor.owl`.
- Admin edits update the ontology-backed catalog so the next search sees the new values.
- JADE agents handle the recommendation flow and communicate through ACL messages.

## Project Layout
```
src/
  main/
    java/org/example/motoadvisor/   # Application source
    resources/
      ontology/                     # OWL knowledge base
      templates/                    # Thymeleaf views
      application.properties        # Config
  test/
    java/org/example/motoadvisor/   # Tests
MVP_SCOPE.md         # Frozen MVP scope — read before changing anything
MVP_ACCEPTANCE.md    # Binary pass/fail criteria for sign-off
BACKLOG.md           # Post-MVP ideas
DECISIONS.md         # Architecture Decision Records
```

## Contributing
All changes during MVP must comply with the rules in [MVP_SCOPE.md](MVP_SCOPE.md).
Post-MVP ideas go in [BACKLOG.md](BACKLOG.md), not in the codebase.
Acceptance criteria (pass/fail sign-off) are in [MVP_ACCEPTANCE.md](MVP_ACCEPTANCE.md).

