package dev.agiro.healthygation.graphrag.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "healthygation.graph-rag")
public class GraphRagProperties {

    @NotNull
    private String neo4jUri;

    @NotNull
    private String neo4jUsername;

    @NotNull
    private String neo4jPassword;

    @NotNull
    private Integer embeddingDimensions = 1536;

    @NotNull
    private Integer similarityTopK = 5;

    @NotNull
    private Boolean cypherGenerationEnabled = false;

    public String getNeo4jUri() {
        return neo4jUri;
    }

    public void setNeo4jUri(String neo4jUri) {
        this.neo4jUri = neo4jUri;
    }

    public String getNeo4jUsername() {
        return neo4jUsername;
    }

    public void setNeo4jUsername(String neo4jUsername) {
        this.neo4jUsername = neo4jUsername;
    }

    public String getNeo4jPassword() {
        return neo4jPassword;
    }

    public void setNeo4jPassword(String neo4jPassword) {
        this.neo4jPassword = neo4jPassword;
    }

    public Integer getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public void setEmbeddingDimensions(Integer embeddingDimensions) {
        this.embeddingDimensions = embeddingDimensions;
    }

    public Integer getSimilarityTopK() {
        return similarityTopK;
    }

    public void setSimilarityTopK(Integer similarityTopK) {
        this.similarityTopK = similarityTopK;
    }

    public Boolean getCypherGenerationEnabled() {
        return cypherGenerationEnabled;
    }

    public void setCypherGenerationEnabled(Boolean cypherGenerationEnabled) {
        this.cypherGenerationEnabled = cypherGenerationEnabled;
    }
}
