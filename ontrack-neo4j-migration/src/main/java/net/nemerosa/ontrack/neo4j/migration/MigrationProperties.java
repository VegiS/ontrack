package net.nemerosa.ontrack.neo4j.migration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "ontrack.migration")
@Component
@Data
public class MigrationProperties {

    /**
     * Projects to include (all by default)
     */
    private List<String> includes = new ArrayList<>();

    /**
     * Projects to include (none by default)
     */
    private List<String> excludes = new ArrayList<>();

    /**
     * Branch filter
     */
    private String branchExpression = ".*";

    /**
     * Tests if a project must be included or not
     */
    public boolean includesProject(String name) {
        return doIncludeProject(name) && !doExcludeProject(name);
    }

    private boolean doExcludeProject(String name) {
        return excludes.contains(name);
    }

    private boolean doIncludeProject(String name) {
        return includes.isEmpty() || includes.contains(name);
    }

}
