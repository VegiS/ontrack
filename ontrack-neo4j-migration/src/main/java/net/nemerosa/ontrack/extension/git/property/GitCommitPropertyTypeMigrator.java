package net.nemerosa.ontrack.extension.git.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import net.nemerosa.ontrack.json.JsonUtils;
import net.nemerosa.ontrack.neo4j.migration.Entity;
import net.nemerosa.ontrack.neo4j.migration.PropertyMigrator;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.io.IOException;

@SuppressWarnings("unused")
public class GitCommitPropertyTypeMigrator implements PropertyMigrator {
    @Override
    public void migrate(String type, JsonNode data, Entity entity, Neo4jOperations template) throws IOException {
        String commit = JsonUtils.get(data, "commit");
        String cypherQuery = String.format(
                " MATCH (b: Build {id: {entityId}}) " +
                        "SET b.`%s` = {commit}",
                type
        );

        template.query(
                cypherQuery,
                ImmutableMap.<String, Object>builder()
                        .put("entityId", entity.getId())
                        .put("commit", commit)
                        .build()
        );
    }
}
