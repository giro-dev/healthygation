# Plan -- POC-20260731-1856

## Ticket Details

| Field | Value |
|-------|-------|
| **Ticket ID** | POC-20260731-1856 |
| **Title** | Graph-RAG for patient cohort pattern discovery |
| **Type** | PoC / Spike |
| **Priority** | Medium |
| **Assigned To** | Devin |
| **Status** | In Progress |
| **Sprint** | N/A |

## Description

Build a proof-of-concept that ingests FHIR Patient, Condition, MedicationRequest and Observation resources into a Neo4j property graph, computes vector embeddings for the clinical text using Spring AI/OpenAI, and exposes a natural-language `POST /graph-rag/ask` endpoint that can discover patterns across a patient cohort.

## Acceptance Criteria

- [ ] Neo4j service can be started with Docker Compose.
- [ ] Maven build compiles after adding Neo4j and validation dependencies.
- [ ] A batch of 100-1,000 synthetic Synthea patients can be ingested from the existing HAPI FHIR container into Neo4j.
- [ ] `POST /graph-rag/ask` returns coherent, evidence-backed answers for at least 3 pattern questions.
- [ ] No real patient PII is processed or sent to OpenAI.

## Refined Requirements (SMART)

### Functional Requirements
1. Ingest Patients, Conditions, MedicationRequests and Observations from the FHIR server into a Neo4j graph.
2. Attach an `Embedding` label and a `vector` embedding to each clinical concept node.
3. Create a vector index over the `embedding` property.
4. Accept a natural-language question, embed it, retrieve similar nodes, expand one hop in the graph, build a context prompt and call the OpenAI chat model.
5. (Optional) Generate safe, parameterized Cypher for cohort-counting questions using few-shot prompting.
6. Return the answer plus the Cypher or retrieved subgraph context used.

### Non-Functional Requirements
1. Use only the Synthea synthetic data already served by `smartonfhir/hapi-5:r4-synthea`.
2. Parameterize every Cypher query; never concatenate user input.
3. Keep ingestion of 100 patients under 60 seconds and a query under 10 seconds on a PoC laptop.
4. Follow package-by-feature structure, constructor injection, no Lombok.

### Edge Cases & Special Conditions
1. FHIR resource missing a display text (use code `coding.display` or system+code fallback).
2. Empty bundle from FHIR server.
3. LLM returns non-executable Cypher (validate against a whitelist of labels/relationships, fall back to vector retrieval).
4. Neo4j unavailable (return a clear `ProblemDetail` error).

## Implementation Plan

### Branch
- **Name**: `feature/POC-20260731-1856-graph-rag-patients`
- **Base**: `main`

### Tasks / Milestones

| # | Task | Estimate | Dependencies | Risk |
|---|------|----------|--------------|------|
| 1 | Add Neo4j service to `compose.yml` and `pom.xml` dependencies | 1h | None | Low |
| 2 | Add `graphrag` package and `Neo4jConfig` / `GraphRagProperties` | 1h | Task 1 | Low |
| 3 | Implement `Neo4jGraphStore` (schema, vector index, CRUD, similar-node search, parameterized Cypher) | 3h | Task 2 | Medium |
| 4 | Extend `FhirClientService` to fetch Conditions, MedicationRequests and Observations by Patient | 2h | None | Low |
| 5 | Implement `FhirGraphIngester` (anonymize, build nodes, compute embeddings, write in batches) | 3h | Task 3, 4 | Medium |
| 6 | Implement `GraphRagService` (embed question, retrieve, expand, generate/validate Cypher, LLM prompt) | 4h | Task 3, 5 | High |
| 7 | Create `GraphRagController` and request/response records | 2h | Task 6 | Low |
| 8 | Add exception handling and `graph-rag.http` test script | 1h | Task 7 | Low |
| 9 | Build, run Docker Compose, ingest data and run the 3 pattern questions | 2h | Task 1-8 | Medium |

### Detailed Implementation Steps

#### 1. `compose.yml` -- add Neo4j service
Add a `neo4j` service (image `neo4j:5.23.0-community` or similar) with `NEO4J_AUTH`, ports `7474` and `7687`. The HAPI FHIR container stays unchanged.

#### 2. `pom.xml` -- new dependencies
Add `org.neo4j.driver:neo4j-java-driver:5.23.0` and `org.springframework.boot:spring-boot-starter-validation` (for typed config). Keep `spring-ai-starter-model-openai`. No Lombok.

#### 3. `application.yaml` -- Neo4j and graph-rag configuration
Add `spring.neo4j.uri`, `spring.neo4j.authentication.username`, `spring.neo4j.authentication.password`. Add `healthygation.graph-rag.embedding-dimensions=1536`, `similarity-top-k=5`, `cypher-generation-enabled=true`.

#### 4. `graphrag.config.Neo4jConfig`
Create a `Driver` bean using `GraphDatabase.driver(uri, AuthTokens.basic(...))`. Constructor-inject `GraphRagProperties`.

#### 5. `graphrag.config.GraphRagProperties`
`@ConfigurationProperties(prefix = "healthygation.graph-rag")` class with `uri`, `username`, `password`, `embeddingDimensions`, `similarityTopK`, `cypherGenerationEnabled`. Use `jakarta.validation.constraints.NotNull` and `@Validated`.

#### 6. `graphrag.domain.Neo4jGraphStore`
- `createSchema()` -- create constraints and a vector index `embedding-index` for label `Embedding` on property `embedding`.
- `saveBatch(...)` using `UNWIND`.
- `findSimilarNodes(float[] embedding, int k)` using `db.index.vector.queryNodes`.
- `query(String cypher, Map<String,Object> parameters)` -- always use parameters; no string concatenation.
- `expandOneHop(Set<String> nodeIds)` -- follow all relationships one hop.

#### 7. `FhirClientService` extension
Add `searchConditionsByPatient(String patientId)`, `searchMedicationRequestsByPatient(String patientId)`, `searchObservationsByPatient(String patientId)`. Return `Bundle`. Keep existing methods intact.

#### 8. `graphrag.domain.FhirGraphIngester`
- Fetch each page of Patients using `fhirClientService.fetchPage`.
- For each patient, fetch Conditions, MedicationRequests and Observations.
- Anonymize patient nodes: store only `patientId` (internal UUID), `gender`, `birthYear` (not exact birth date), no names.
- Build nodes with `text` (display or coding text) and `embedding` computed via `EmbeddingClient`.
- Batch save nodes and relationships to Neo4j. Run on startup behind a `@Profile("ingest")` `CommandLineRunner` bean.

#### 9. `graphrag.domain.GraphRagService`
- `ask(AskRequest)`:
  1. Embed the question.
  2. `Neo4jGraphStore.findSimilarNodes` to get top-k concept nodes.
  3. `Neo4jGraphStore.expandOneHop` to gather patient/context.
  4. If `cypherGenerationEnabled`, call `CypherGenerationService` with a few-shot prompt and strict label/relationship whitelist to produce safe Cypher; execute it; include counts/tables in the prompt.
  5. Build the final prompt with context and call `ChatClient`.
  6. Return `AskResponse` with `answer` and `context` or `executedCypher`.

#### 10. `graphrag.domain.CypherGenerationService` (optional)
Use `ChatClient` with few-shot examples. Parse the returned Cypher; validate with an allow-list of labels (`Patient`, `Condition`, `Medication`, `Observation`, `Embedding`) and relationship types; parameterize all literals; reject any `CALL`, `LOAD CSV`, `apoc`, or command clauses; fallback to vector retrieval if validation fails.

#### 11. `graphrag.api.GraphRagController`
- `POST /graph-rag/ask` with `AskRequest` record (`question`) and `AskResponse` record (`answer`, `context`, `cypher`).
- Use constructor-injected `GraphRagService`.
- Reuse existing `ControllerAdvise` for global exception handling.

#### 12. Testing
- Unit tests for `FhirGraphIngester` and `GraphRagService` using Mockito.
- `src/test/resources/graph-rag.http` with 3 sample pattern questions.
- Manual integration test via Docker Compose and the `./mvnw spring-boot:run` profile.

### Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| PII sent to OpenAI | High | Low | Use only Synthea data; anonymize before embedding; do not send names/identifiers |
| Build incompatibility with Spring Boot 4.1.0 / Java 25 | High | Medium | Pin known-good versions; run `mvn clean verify` immediately after dependency changes |
| Cypher injection / malicious LLM output | High | Low | Whitelist labels/relationships; use parameterized queries; never execute raw user input |
| Hallucinated or non-executable Cypher | Medium | Medium | Few-shot prompting; strict validation; fallback to vector + traversal |
| Slow ingestion for large cohorts | Medium | Medium | Batch writes (UNWIND); async processing; limit to 1,000 patients for PoC |

## SDLC Progress

- [X] Stage 0: PLAN -- Completed (POC scope defined above)
- [X] Stage 1: SETUP -- Completed
- [X] Stage 2: CODE -- Completed
- [X] Stage 3: BUILD -- Completed
- [X] Stage 4: TEST -- Completed
- [ ] Stage 5: RELEASE -- Pending
- [ ] Stage 6: DEPLOY -- Pending
