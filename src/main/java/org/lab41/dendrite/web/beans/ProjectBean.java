package org.lab41.dendrite.web.beans;

import org.hibernate.validator.constraints.NotEmpty;
import org.lab41.dendrite.web.beans.GraphBean;

import javax.validation.constraints.NotNull;

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
