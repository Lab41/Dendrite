package org.lab41.dendrite.web.beans;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class ProjectBean {
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
