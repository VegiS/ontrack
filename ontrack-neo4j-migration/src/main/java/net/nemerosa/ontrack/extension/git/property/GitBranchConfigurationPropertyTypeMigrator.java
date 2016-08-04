package net.nemerosa.ontrack.extension.git.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import net.nemerosa.ontrack.json.JsonUtils;
import net.nemerosa.ontrack.neo4j.migration.Entity;
import net.nemerosa.ontrack.neo4j.migration.PropertyMigrator;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.io.IOException;

@SuppressWarnings("unused")
public class GitBranchConfigurationPropertyTypeMigrator implements PropertyMigrator {
    @Override
    public void migrate(String type, JsonNode data, Entity entity, Neo4jOperations template) throws IOException {
        String branch = JsonUtils.get(data, "branch");
        // FIXME Link configuration
        boolean override = JsonUtils.getBoolean(data, "override");
        int buildTagInterval = JsonUtils.getInt(data, "buildTagInterval");

        String cypherQuery = String.format(
                " MATCH (b: Branch {id: {entityId}}) " +
                        "CREATE (b)-[:HAS_PROPERTY]->(p: `%s` {branch: {branch}," +
                        " override: {override}, buildTagInterval: {buildTagInterval}" +
                        "}) ",
                type
        );

        template.query(
                cypherQuery,
                ImmutableMap.<String, Object>builder()
                        .put("entityId", entity.getId())
                        .put("type", type)
                        .put("branch", branch)
                        .put("override", override)
                        .put("buildTagInterval", buildTagInterval)
                        .build()
        );
    }
}
