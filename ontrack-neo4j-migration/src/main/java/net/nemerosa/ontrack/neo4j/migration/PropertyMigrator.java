package net.nemerosa.ontrack.neo4j.migration;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.io.IOException;

public interface PropertyMigrator {

    void migrate(String type, JsonNode data, Entity entity, Neo4jOperations template) throws IOException;

}
