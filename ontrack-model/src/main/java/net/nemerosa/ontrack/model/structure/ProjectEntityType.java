package net.nemerosa.ontrack.model.structure;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Enumeration of all {@link net.nemerosa.ontrack.model.structure.ProjectEntity} types together
 * with the way to load them from the {@link net.nemerosa.ontrack.model.structure.StructureService} service.
 */
public enum ProjectEntityType {

    PROJECT("project", "Project", StructureService::getProject),

    BRANCH("branch", "Branch", StructureService::getBranch),

    PROMOTION_LEVEL("promotion level", "PromotionLevel", StructureService::getPromotionLevel),

    VALIDATION_STAMP("validation stamp", "ValidationStamp", StructureService::getValidationStamp),

    BUILD("build", "Build", StructureService::getBuild),

    PROMOTION_RUN("promotion run", "PromotionRun", StructureService::getPromotionRun),

    VALIDATION_RUN("validation run", "ValidationRun", StructureService::getValidationRun);

    private final String displayName;
    private final String nodeName;
    private final BiFunction<StructureService, ID, ProjectEntity> entityFn;

    ProjectEntityType(String displayName, String nodeName, BiFunction<StructureService, ID, ProjectEntity> entityFn) {
        this.displayName = displayName;
        this.nodeName = nodeName;
        this.entityFn = entityFn;
    }

    public Function<ID, ProjectEntity> getEntityFn(StructureService structureService) {
        return id -> entityFn.apply(structureService, id);
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
