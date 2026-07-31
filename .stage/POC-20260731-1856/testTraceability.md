# Test Traceability -- POC-20260731-1856

| Requirement from `plan.md` | Test Case(s) | Verification |
|----------------------------|--------------|--------------|
| `POST /graph-rag/ask` returns coherent, evidence-backed answers | TC-01, TC-04 | Controller/Service tests |
| 100-1,000 synthetic patients ingestible from HAPI FHIR | TC-05 | Ingester unit test |
| No real patient PII is processed | Code review + TC-05 (no name in patient node) | Ingester assertion |
| Natural-language question accepted | TC-01, TC-02, TC-03 | Controller/Service tests |
| Parameterized Cypher and safe labels | Code review | Static analysis N/A; reviewed in implementation |

## Files Referenced

- `GraphRagControllerTest.java` -- TC-01, TC-02, TC-06
- `GraphRagServiceTest.java` -- TC-03, TC-04
- `FhirGraphIngesterTest.java` -- TC-05
