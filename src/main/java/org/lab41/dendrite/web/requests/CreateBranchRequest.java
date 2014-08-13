package org.lab41.dendrite.web.requests;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class CreateBranchRequest {
    @NotNull
    @NotEmpty
    private String graphId;

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }
}
