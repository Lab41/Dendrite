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

import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class GetBranchResponse {
    private String _id;
    private String name;
    private String creationTime;
    private String projectId;
    private String graphId;
    private String jobId;

    public GetBranchResponse(BranchMetadata branchMetadata) {
        this(branchMetadata, null);
    }

    public GetBranchResponse(BranchMetadata branchMetadata, JobMetadata jobMetadata) {
        this._id = branchMetadata.getId().toString();
        this.name = branchMetadata.getName();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date creationTime = branchMetadata.getCreationTime();
        if (creationTime != null) {
            this.creationTime = df.format(creationTime);
        }

        ProjectMetadata projectMetadata = branchMetadata.getProject();
        if (projectMetadata != null) {
            this.projectId = branchMetadata.getProject().getId().toString();
        }

        GraphMetadata graphMetadata = branchMetadata.getGraph();
        if (graphMetadata != null) {
            this.graphId = branchMetadata.getGraph().getId().toString();
        }

        if (jobMetadata != null) {
            this.jobId = jobMetadata.getId().toString();
        }
    }

    public String get_id() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getGraphId() {
        return graphId;
    }

    public String getJobId() {
        return jobId;
    }

}
