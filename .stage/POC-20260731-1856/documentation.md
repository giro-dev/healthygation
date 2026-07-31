# Graph-RAG PoC Documentation -- POC-20260731-1856

## Overview

This PoC adds a Graph-RAG capability to `healthygation`. It ingests synthetic patient data from the existing HAPI FHIR server into a Neo4j property graph, computes embeddings for clinical concepts and exposes a natural-language endpoint to discover patterns across a patient cohort.

## Architecture

```
FHIR (HAPI) -> FhirGraphIngester -> Neo4j (Patient/Condition/Medication/Observation)
                                       ^
                                       | vector index + one-hop traversal
User question -> EmbeddingModel -> GraphRagService -> ChatClient (OpenAI) -> answer
```

## Endpoints

### `POST /graph-rag/ask`

Accepts a natural-language question and returns a concise answer based on the patient cohort graph.

**Request:**
```json
{
  "question": "What conditions appear together with hypertension?"
}
```

**Response:**
```json
{
  "answer": "...",
  "context": "...",
  "cypher": null
}
```

## Ingestion

Start the application with the `ingest` Spring profile to load up to 1,000 patients from the HAPI FHIR container:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=ingest"
```

After ingestion, stop the process and start normally:

```bash
./mvnw spring-boot:run
```

## Docker Compose

```bash
docker compose up -d
```

This starts HAPI FHIR on port 8085 and Neo4j on ports 7474 and 7687.

## Configuration

All settings are in `application.yaml` under `healthygation.graph-rag` and can be overridden via environment variables:

- `NEO4J_URI` -- default `bolt://localhost:7687`
- `NEO4J_USERNAME` -- default `neo4j`
- `NEO4J_PASSWORD` -- default `healthygation`
- `OPENAI_API_KEY` -- required for embeddings and chat

## Security Notes

- Only synthetic Synthea data from the HAPI FHIR container is used.
- Patient names and identifiers are not stored in the graph.
- All Cypher queries use parameters; dynamic labels and relationship types are whitelisted.
- The `cypherGenerationEnabled` flag is `false` by default to avoid LLM-generated Cypher risks.
