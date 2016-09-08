package net.nemerosa.ontrack.dsl

import net.nemerosa.ontrack.dsl.doc.DSL
import net.nemerosa.ontrack.dsl.doc.DSLMethod
import net.nemerosa.ontrack.dsl.properties.ProjectProperties

@DSL
class Project extends AbstractProjectResource {

    Project(Ontrack ontrack, Object node) {
        super(ontrack, node)
    }

    def call(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = this
        closure()
    }

    @DSLMethod("Gets the list of branches for the project.")
    List<Branch> getBranches() {
        ontrack.get(link('branches')).resources.collect {
            new Branch(ontrack, it)
        }
    }

    @DSLMethod(value = "Retrieves or creates a branch for the project", count = 3)
    Branch branch(String name, String description = '', boolean getIfExists = false) {
        def node = ontrack.get(link('branches')).resources.find { it.name == name }
        if (node) {
            if (getIfExists) {
                new Branch(
                        ontrack,
                        ontrack.get(node._self)
                )
            } else {
                throw new ObjectAlreadyExistsException("Branch ${name} already exists.")
            }
        } else {
            new Branch(
                    ontrack,
                    ontrack.post(link('createBranch'), [
                            name       : name,
                            description: description
                    ])
            )
        }
    }

    @DSLMethod(value = "Retrieves or creates a branch for the project, and then configures it.", id = "branch-closure", count = 4)
    Branch branch(String name, String description = '', boolean getIfExists = false, Closure closure) {
        Branch b = branch(name, description, getIfExists)
        b(closure)
        b
    }

    @DSLMethod("Access to the project properties")
    ProjectProperties getConfig() {
        new ProjectProperties(ontrack, this)
    }

    @DSLMethod("Searches for builds in the project.")
    List<Build> search(Map<String, ?> form) {
        def url = query(
                "${link('buildSearch')}/search",
                form
        )
        ontrack.get(url).resources.collect { new Build(ontrack, it.build) }
    }


}
