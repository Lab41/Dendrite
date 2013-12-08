package org.lab41.dendrite.web.controller;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ProjectBean {
    @NotNull
    @NotEmpty
    private String name;

    @NotNull
    private GraphBean graph;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public GraphBean getGraph() {
        return graph;
    }

    public void setGraph(GraphBean graph) {
        this.graph = graph;
    }


}
