package net.nemerosa.ontrack.extension.jenkins;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import net.nemerosa.ontrack.json.JsonUtils;
import net.nemerosa.ontrack.neo4j.migration.Entity;
import net.nemerosa.ontrack.neo4j.migration.PropertyMigrator;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.io.IOException;

@SuppressWarnings("unused")
public class JenkinsJobPropertyTypeMigrator implements PropertyMigrator {

    @Override
    public void migrate(String type, JsonNode data, Entity entity, Neo4jOperations template) throws IOException {
        String job = JsonUtils.get(data, "job");
        String configurationName = JsonUtils.get(data, "configuration");

        String cypherQuery = String.format(
                " MATCH (c:`net.nemerosa.ontrack.extension.jenkins.JenkinsConfiguration` {name: {configurationName}}), " +
                        " (b: Build {id: {entityId}}) " +
                        " CREATE (b)-[:HAS_PROPERTY]->(p: `%s` {job: {job}})-[:CONFIGURED_BY]->(c) ",
                type
        );

        template.query(
                cypherQuery,
                ImmutableMap.<String, Object>builder()
                        .put("entityId", entity.getId())
                        .put("job", job)
                        .put("configurationName", configurationName)
                        .build()
        );
    }
}
