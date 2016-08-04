package net.nemerosa.ontrack.extension.svn.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import net.nemerosa.ontrack.json.JsonUtils;
import net.nemerosa.ontrack.neo4j.migration.Entity;
import net.nemerosa.ontrack.neo4j.migration.PropertyMigrator;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.io.IOException;

@SuppressWarnings("unused")
public class SVNBranchConfigurationPropertyTypeMigrator implements PropertyMigrator {
    @Override
    public void migrate(String type, JsonNode data, Entity entity, Neo4jOperations template) throws IOException {
        String branchPath = JsonUtils.get(data, "branchPath");

        // FIXME Link configuration

        String cypherQuery = String.format(
                " MATCH (b: Branch {id: {entityId}}) " +
                        " CREATE (b)-[:HAS_PROPERTY]->(p: `%s` {branchPath: {branchPath}}) ",
                type
        );

        template.query(
                cypherQuery,
                ImmutableMap.<String, Object>builder()
                        .put("entityId", entity.getId())
                        .put("type", type)
                        .put("branchPath", branchPath)
                        .build()
        );
    }
}
