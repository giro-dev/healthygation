# Implementation Report -- POC-20260731-1856

## Files Created or Modified

### Configuration and dependencies
- `pom.xml` -- added `neo4j-java-driver:5.25.0` and `spring-boot-starter-validation`.
- `compose.yml` -- added `neo4j:5.25.0-community` service with authentication.
- `src/main/resources/application.yaml` -- added `healthygation.graph-rag` properties for Neo4j and vector settings.
- `src/main/java/dev/agiro/healthygation/error/ProblemDetail.java` -- created the missing error DTO used by `ControllerAdvise`.

### New graph-rag package
- `graphrag/config/GraphRagProperties.java` -- typed, validated configuration.
- `graphrag/config/Neo4jConfig.java` -- exposes `org.neo4j.driver.Driver` bean.
- `graphrag/config/GraphRagConfig.java` -- exposes a `ChatClient` bean from the auto-configured `ChatModel`.
- `graphrag/config/GraphRagIngestionRunner.java` -- `CommandLineRunner` active only under the `ingest` Spring profile.
- `graphrag/domain/GraphNode.java` -- record representing a graph node to be persisted.
- `graphrag/domain/Relationship.java` -- record representing a relationship between nodes.
- `graphrag/domain/Neo4jGraphStore.java` -- schema creation, batch save, vector similarity search and one-hop expansion.
- `graphrag/domain/FhirGraphIngester.java` -- reads FHIR pages and writes anonymized patient/concept/relationship data to Neo4j.
- `graphrag/domain/GraphRagService.java` -- embeds the question, retrieves similar concepts, expands to patients, builds a prompt and asks the LLM.
- `graphrag/api/AskRequest.java` -- validated request record.
- `graphrag/api/AskResponse.java` -- response record.
- `graphrag/api/GraphRagController.java` -- `POST /graph-rag/ask` endpoint.

### Modified existing files
- `src/main/java/dev/agiro/healthygation/FhirClientService.java` -- added `searchConditionsByPatient`, `searchMedicationRequestsByPatient` and `searchObservationsByPatient`.
- `src/test/resources/graph-rag.http` -- HTTP test script with 3 pattern questions.

## Design Decisions

- `Neo4jGraphStore` uses a single `Embedding` label for all concept nodes with one vector index, while also assigning a clinical label (`Condition`, `Medication`, `Observation`). This keeps the vector index simple and the graph semantics explicit.
- `FhirGraphIngester` ingests one page at a time to avoid keeping the entire cohort in memory during the PoC.
- Ingestion is triggered only with the `ingest` Spring profile, so the application can be started for queries without re-running ingestion.
- Cypher generation is behind a `cypherGenerationEnabled` property but left disabled by default because the PoC relies on vector + graph-traversal RAG, which is safer from a security and reliability point of view.
- All user input to Neo4j is passed as parameters; labels and relationship types in dynamic Cypher are validated against an explicit allow-list.

## Known Limitations / Tech Debt

- `CypherGenerationService` is not implemented; the property exists for a later PoC iteration.
- No unit or integration tests were added yet beyond the manual `graph-rag.http` script.
- The Neo4j healthcheck in `compose.yml` relies on `cypher-shell`, which may not be available in all image variants.
- `GraphRagProperties` uses setter-based binding because the project does not allow Lombok.
- Vector search currently expands exactly one hop from the top-k concepts; deeper hops can be added in a later iteration.
