package net.nemerosa.ontrack.dsl

import net.nemerosa.ontrack.dsl.doc.DSL

@DSL
class SearchResult extends AbstractResource {

    SearchResult(Ontrack ontrack, Object node) {
        super(ontrack, node)
    }

    @DSL(value = "Gets the display name for the search result.")
    String getTitle() {
        node['title']
    }

    @DSL(value = "Gets a description for the search result.")
    String getDescription() {
        node['description']
    }

    @DSL(value = "Gets the URI to access the search result details (API).")
    String getUri() {
        node['uri']
    }

    @DSL(value = "Gets the URI to display the search result details (Web).")
    String getPage() {
        node['page']
    }

    @DSL(value = "Gets a percentage of accuracy about the result.")
    int getAccuracy() {
        node['accuracy'] as int
    }

}
