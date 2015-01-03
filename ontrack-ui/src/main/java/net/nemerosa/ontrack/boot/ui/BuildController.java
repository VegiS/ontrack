package net.nemerosa.ontrack.boot.ui;

import net.nemerosa.ontrack.model.Ack;
import net.nemerosa.ontrack.model.form.Form;
import net.nemerosa.ontrack.model.form.Int;
import net.nemerosa.ontrack.model.form.Text;
import net.nemerosa.ontrack.model.security.SecurityService;
import net.nemerosa.ontrack.model.structure.*;
import net.nemerosa.ontrack.ui.controller.AbstractResourceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/structure")
public class BuildController extends AbstractResourceController {

    private final StructureService structureService;
    private final PropertyService propertyService;
    private final SecurityService securityService;

    @Autowired
    public BuildController(StructureService structureService, PropertyService propertyService, SecurityService securityService) {
        this.structureService = structureService;
        this.propertyService = propertyService;
        this.securityService = securityService;
    }

    @RequestMapping(value = "project/{projectId}/builds", method = RequestMethod.GET)
    public Form buildSearchForm(@PathVariable ID projectId) {
        return Form.create()
                .with(
                        Int.of("count")
                                .label("Maximum number")
                                .help("Maximum number of builds to return.")
                                .min(1)
                                .value(10)
                )
                .with(
                        Text.of("branchName")
                                .label("Branch name")
                                .help("Regular expression for the branch name")
                                .optional()
                )
                .with(
                        Text.of("buildName")
                                .label("Build name")
                                .help("Regular expression for the build name")
                                .optional()
                )
                .with(
                        Text.of("promotionName")
                                .label("Promotion name")
                                .help("Collects only builds which are promoted to this promotion level.")
                                .optional()
                )
                .with(
                        Text.of("validationStampName")
                                .label("Validation stamp name")
                                .help("Collects only builds which have `passed` this validation stamp.")
                                .optional()
                )
                ;
    }

    @RequestMapping(value = "branches/{branchId}/builds/create", method = RequestMethod.GET)
    public Form newBuildForm(@PathVariable ID branchId) {
        // Checks the branch does exist
        structureService.getBranch(branchId);
        // Returns the form
        return Build.form();
    }

    @RequestMapping(value = "branches/{branchId}/builds/create", method = RequestMethod.POST)
    public Build newBuild(@PathVariable ID branchId, @RequestBody @Valid BuildRequest request) {
        // Gets the holding branch
        Branch branch = structureService.getBranch(branchId);
        // Build signature
        Signature signature = securityService.getCurrentSignature();
        // Creates a new build
        Build build = Build.of(branch, request.asNameDescription(), signature);
        // Saves it into the repository
        build = structureService.newBuild(build);
        // Saves the properties
        for (PropertyCreationRequest propertyCreationRequest : request.getProperties()) {
            propertyService.editProperty(
                    build,
                    propertyCreationRequest.getPropertyTypeName(),
                    propertyCreationRequest.getPropertyData()
            );
        }
        // OK
        return build;
    }

    @RequestMapping(value = "builds/{buildId}/update", method = RequestMethod.GET)
    public Form updateBuildForm(@PathVariable ID buildId) {
        return structureService.getBuild(buildId).asForm();
    }

    @RequestMapping(value = "builds/{buildId}/update", method = RequestMethod.PUT)
    public Build updateBuild(@PathVariable ID buildId, @RequestBody @Valid NameDescription nameDescription) {
        // Gets from the repository
        Build build = structureService.getBuild(buildId);
        // Updates
        build = build.update(nameDescription);
        // Saves in repository
        structureService.saveBuild(build);
        // As resource
        return build;
    }

    @RequestMapping(value = "builds/{buildId}", method = RequestMethod.DELETE)
    public Ack deleteBuild(@PathVariable ID buildId) {
        return structureService.deleteBuild(buildId);
    }

    @RequestMapping(value = "builds/{buildId}", method = RequestMethod.GET)
    public Build getBuild(@PathVariable ID buildId) {
        return structureService.getBuild(buildId);
    }

}
