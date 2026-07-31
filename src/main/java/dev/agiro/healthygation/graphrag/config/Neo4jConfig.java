package dev.agiro.healthygation.graphrag.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphRagProperties.class)
public class Neo4jConfig {

    private final GraphRagProperties properties;

    public Neo4jConfig(GraphRagProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(
                properties.getNeo4jUri(),
                AuthTokens.basic(properties.getNeo4jUsername(), properties.getNeo4jPassword())
        );
    }
}
