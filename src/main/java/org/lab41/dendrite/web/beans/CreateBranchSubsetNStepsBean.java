package org.lab41.dendrite.web.beans;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class CreateBranchSubsetNStepsBean {
    @NotNull
    @NotEmpty
    private String query;

    @NotNull
    private Integer steps;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }
}
