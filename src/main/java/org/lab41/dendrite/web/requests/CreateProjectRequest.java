package org.lab41.dendrite.web.requests;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class CreateProjectRequest {
    @NotNull
    @NotEmpty
    private String name;

    private boolean createGraph = true;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public boolean createGraph() {
        return createGraph;
    }

    public void setCreateGraph(boolean createGraph) {
        this.createGraph = createGraph;
    }
}
