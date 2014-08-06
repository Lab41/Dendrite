/*
 * Copyright 2014 In-Q-Tel/Lab41
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab41.dendrite.web.responses;

import org.lab41.dendrite.jobs.BranchCommitJob;
import org.lab41.dendrite.jobs.BranchCommitSubsetJob;

public class BranchJobResponse {

    private String projectId;
    private String branchId;
    private String graphId;
    private String jobId;

    public BranchJobResponse(BranchCommitJob job) {
        projectId = job.getProjectId();
        branchId = job.getBranchId();
        graphId = job.getDstGraphId();
        jobId = job.getJobId();
    }

    public BranchJobResponse(BranchCommitSubsetJob job) {
        projectId = job.getProjectId();
        branchId = job.getBranchId();
        graphId = job.getDstGraphId();
        jobId = job.getJobId();
    }

    public String getStatus() {
        return "ok";
    }

    public String getMsg() {
        return "job submitted";
    }

    public String getProjectId() {
        return projectId;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getGraphId() {
        return graphId;
    }

    public String getJobId() {
        return jobId;
    }

}
