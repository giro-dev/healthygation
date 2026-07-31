# Build Report -- POC-20260731-1856

## Checks

| Check | Status | Details |
|-------|--------|---------|
| Compile | PASS | `mvn clean compile -DskipTests` succeeded with Java 25.0.4. |
| Lint | N/A | No checkstyle/PMD/spotless plugin configured in `pom.xml`. Compiler produced no warnings. |
| Type Check | PASS | Covered by the successful compile step with `javac` release 25. |
| Dependency Audit | N/A | No OWASP dependency-check or Maven `enforcer` plugin configured. All dependencies resolved from Maven Central. |
| Artifact | PASS | `mvn package -DskipTests` produced `target/healthygation-0.0.1-SNAPSHOT.jar`. |

## Build Commands Run

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
export PATH=$JAVA_HOME/bin:$PATH
./mvnw clean compile -DskipTests
./mvnw package -DskipTests
```

## Artifact

- **Path**: `target/healthygation-0.0.1-SNAPSHOT.jar`
- **Repackaged with nested dependencies**: the original JAR is `healthygation-0.0.1-SNAPSHOT.jar.original`.

## Notes

- The `pom.xml` was reverted to `java.version=25` after installing the Java 25 JDK.
- One code fix was required during build: `EmbeddingModel.embed(...)` returns `float[]`, not `List<Double>`. The conversion was corrected in `FhirGraphIngester` and `GraphRagService`.
