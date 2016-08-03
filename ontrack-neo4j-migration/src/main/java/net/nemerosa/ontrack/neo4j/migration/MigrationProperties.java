package net.nemerosa.ontrack.neo4j.migration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "ontrack.migration")
@Component
@Data
public class MigrationProperties {

    /**
     * Limit for the number of builds being important (-1 for no limit)
     */
    private int buildCount = -1;

}
