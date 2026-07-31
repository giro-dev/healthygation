# Test Design -- POC-20260731-1856

## Scope

This test design covers the Graph-RAG PoC: ingestion of FHIR resources into Neo4j, vector/hybrid retrieval, the `POST /graph-rag/ask` endpoint, and input validation.

## Test Techniques

| Technique | Where Applied |
|-----------|---------------|
| Equivalence partitioning | Valid and invalid `AskRequest` payloads |
| Boundary value analysis | Empty/null question, top-k minimum/maximum |
| State-based / happy path | Successful question answering and ingestion flow |
| Negative / error | Missing Neo4j context, malformed requests |

## Test Cases

| ID | Requirement | Precondition | Input | Expected Result | Priority |
|----|-------------|--------------|-------|-----------------|----------|
| TC-01 | `POST /graph-rag/ask` valid question | Graph has similar concepts | `{"question":"What conditions appear with hypertension?"}` | `200 OK` with non-empty `answer` and `context` | High |
| TC-02 | `POST /graph-rag/ask` empty question | Controller available | `{"question":""}` | `400 Bad Request` | High |
| TC-03 | `GraphRagService.ask` with no matches | Vector search returns empty | Any question | Fallback answer "No relevant context found in the graph." | High |
| TC-04 | `GraphRagService.ask` with matches | Vector returns 2 concepts, 1 patient each | Any question | Answer built from expanded patient context | High |
| TC-05 | `FhirGraphIngester.ingestAll` | One page with 1 patient and 2 conditions | `ingestAll(1, 1)` | `savePatients`, `saveConcepts`, `saveRelationships` called once | Medium |
| TC-06 | `GraphRagController` content-type | Controller available | `POST /graph-rag/ask` without `Content-Type: application/json` | `415 Unsupported Media Type` | Low |

## Notes

- End-to-end Neo4j integration tests are out of scope for the PoC; `Neo4jGraphStore` is tested indirectly through `GraphRagService` mocks.
- The application context test is not run because it requires a running Neo4j and an OpenAI key; the PoC focuses on unit/isolated tests.
