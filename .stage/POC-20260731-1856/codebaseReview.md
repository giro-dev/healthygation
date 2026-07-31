# Codebase Review -- POC-20260731-1856

## Project Overview

| Field | Value |
|-------|-------|
| **Repository** | healthygation |
| **Branch** | feature/POC-20260731-1856-graph-rag-patients |
| **Framework** | Spring Boot 4.1.0 (Java 25) |
| **Build Tool** | Maven |

## Architecture Summary

The project is a thin Spring Boot web service that wraps a HAPI FHIR R4 server. It exposes REST endpoints to list and search patients, returning simple `PatientBrief` records. AI integration is already present through `spring-ai-starter-model-openai`.

## Key Modules & Components

| Module | Purpose | Impact Area |
|--------|---------|-------------|
| `FhirClientService` | FHIR R4 client wrapper | Extended with resource-specific searches for graph ingestion |
| `PatientController` | HTTP layer for patient CRUD/list | Unchanged; new `GraphRagController` follows the same pattern |
| `ControllerAdvise` | Global exception handler | Reused for `GraphRag` exceptions |
| `domain` | Records and enums | New `graphrag` package under `dev.agiro.healthygation` |
| `spring-ai` autoconfig | Chat and embedding models | Used for question embedding and answer generation |

## Coding Conventions

- **Naming**: package-by-feature then package-by-layer; all classes in English.
- **Structure**: `controller/`, `domain/`, `error/` packages; records for DTOs.
- **Testing**: JUnit 5 with `spring-boot-starter-webmvc-test`; HTTP scripts in `src/test/resources/`.
- **Injection**: constructor injection only, no field injection, no Lombok.

## Dependencies & Libraries

| Library | Version | Usage |
|---------|---------|-------|
| Spring Boot | 4.1.0 | Web, config, validation |
| Spring AI | 2.0.0 | OpenAI chat and embeddings |
| HAPI FHIR | 8.8.0 | FHIR R4 client and data structures |
| (new) Neo4j Java Driver | 5.23.0 | Property graph + vector index |
| (new) Spring Boot Validation | (managed) | Typed `@ConfigurationProperties` validation |

## Integration Points

- HAPI FHIR server running on `http://localhost:8085` (Docker Compose `fhir` service).
- OpenAI API via `spring.ai.openai.api-key`.
- (new) Neo4j Bolt on `bolt://localhost:7687`.

## Areas Impacted by This Ticket

| File / Module | Change Type | Risk Level |
|---------------|-------------|------------|
| `compose.yml` | Modified | Low |
| `pom.xml` | Modified | Medium |
| `application.yaml` | Modified | Low |
| `FhirClientService.java` | Modified | Low |
| `src/main/java/dev/agiro/healthygation/graphrag/**` | New | Medium |
| `src/test/resources/graph-rag.http` | New | Low |

## Recommendations

1. Keep the `graphrag` package isolated; do not let Neo4j/HAPI FHIR types leak into the existing `controller`/`domain` packages.
2. Run `mvn clean verify` after each dependency change because the project uses pre-release Java 25 and Spring Boot 4.1.0.
3. Do not commit real `OPENAI_API_KEY`; keep using the environment variable from `application.yaml`.
4. For the PoC, skip Testcontainers and rely on the existing Docker Compose setup to keep build time low.
