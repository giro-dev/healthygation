# Test Results -- POC-20260731-1856

## Execution Summary

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
export PATH=$JAVA_HOME/bin:$PATH
./mvnw test -Dtest=GraphRagServiceTest,GraphRagControllerTest,FhirGraphIngesterTest -DfailIfNoTests=false
```

| Metric | Value |
|--------|-------|
| Tests run | 5 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

## Test Cases Run

### `GraphRagServiceTest`
- `askReturnsNoContextWhenNoSimilarNodes` -- PASS
- `askReturnsAnswerFromContext` -- PASS

### `GraphRagControllerTest`
- `askReturnsOkForValidRequest` -- PASS
- `askReturnsBadRequestForEmptyQuestion` -- PASS

### `FhirGraphIngesterTest`
- `ingestAllProcessesOnePatientWithConditions` -- PASS

## Coverage

No JaCoCo or equivalent coverage tool is configured in the project, so no coverage percentage is available.
Covered components:
- `GraphRagService.ask` -- direct unit tests
- `GraphRagController.ask` -- direct controller tests
- `FhirGraphIngester.ingestAll` -- direct unit tests
- `Neo4jGraphStore` -- covered indirectly through `GraphRagService` and `FhirGraphIngester` mocks

## Notes

- The `HealthygationApplicationTests.contextLoads` test was not executed because the full Spring context requires a running Neo4j and an OpenAI API key, which are not available in the test environment.
- `GraphRagControllerTest` validated that `@NotBlank` on `AskRequest.question` returns `400 Bad Request` for empty input.
