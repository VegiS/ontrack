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

    /**
     * Limit for the number of build links being important (-1 for no limit)
     */
    private int buildLinkCount = -1;

    /**
     * Limit for the number of promotion runs being important (-1 for no limit)
     */
    private int promotionRunCount = -1;

    /**
     * Limit for the number of validation runs being important (-1 for no limit)
     */
    private int validationRunCount = -1;

}
