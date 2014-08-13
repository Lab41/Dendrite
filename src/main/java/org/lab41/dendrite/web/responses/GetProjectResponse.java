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
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class GetProjectResponse {
    private String _id;
    private String name;
    private String creationTime;
    private String current_branch;
    private String current_graph;

    public GetProjectResponse(ProjectMetadata projectMetadata) {
        this._id = projectMetadata.getId().toString();
        this.name = projectMetadata.getName();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date creationTime = projectMetadata.getCreationTime();
        if (creationTime != null) {
            this.creationTime = df.format(creationTime);
        }

        BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
        if (branchMetadata != null) {
            current_branch = branchMetadata.getId().toString();

            GraphMetadata graphMetadata = branchMetadata.getGraph();
            if (graphMetadata != null) {
                this.current_graph = graphMetadata.getId().toString();
            }
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

    public String getCurrent_branch() {
        return current_branch;
    }

    public String getCurrent_graph() {
        return current_graph;
    }
}
