package org.lab41.dendrite.web.requests;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class UpdateCurrentBranchRequest {
    @NotNull
    @NotEmpty
    private String branchName;

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

}
