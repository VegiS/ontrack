package net.nemerosa.ontrack.neo4j.migration;

import lombok.Data;
import net.nemerosa.ontrack.model.structure.ProjectEntityType;

@Data
public class Entity {

    private final ProjectEntityType type;
    private final int id;

}
