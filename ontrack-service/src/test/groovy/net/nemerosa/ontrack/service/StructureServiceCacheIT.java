package net.nemerosa.ontrack.service;

import net.nemerosa.ontrack.it.AbstractServiceTestSupport;
import net.nemerosa.ontrack.model.security.BranchEdit;
import net.nemerosa.ontrack.model.security.ProjectEdit;
import net.nemerosa.ontrack.model.structure.Branch;
import net.nemerosa.ontrack.model.structure.NameDescription;
import net.nemerosa.ontrack.model.structure.Project;
import net.nemerosa.ontrack.model.structure.StructureService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class StructureServiceCacheIT extends AbstractServiceTestSupport {

    @Autowired
    private StructureService structureService;

    @Test
    public void project_cache() throws Exception {
        Project project = doCreateProject();
        asUserWithView(project).execute(() -> {
            Project p1 = structureService.getProject(project.getId());
            Project p2 = structureService.getProject(project.getId());
            assertSame(p1, p2);
        });
    }

    @Test
    public void branch_cache() throws Exception {
        Branch branch = doCreateBranch();
        asUserWithView(branch).with(branch, BranchEdit.class).execute(() -> {
            Branch b1 = structureService.getBranch(branch.getId());
            Branch b2 = structureService.getBranch(branch.getId());
            assertSame(b1, b2);
            b2 = b2.update(NameDescription.nd(b1.getName(), "Updated branch").asState());
            structureService.saveBranch(b2);
            b2 = structureService.getBranch(branch.getId());
            assertNotSame(b1, b2);
        });
    }

}
