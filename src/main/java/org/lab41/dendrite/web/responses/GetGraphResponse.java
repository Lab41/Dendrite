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

import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public class GetGraphResponse {
    private String _id;
    private String creationTime;
    private Properties properties;
    private String projectId;
    private String parentGraphId;

    public GetGraphResponse(GraphMetadata graphMetadata) {
        this._id = graphMetadata.getId().toString();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date creationTime = graphMetadata.getCreationTime();
        if (creationTime != null) {
            this.creationTime = df.format(creationTime);
        }

        this.properties = graphMetadata.getProperties();

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata != null) {
            this.projectId = projectMetadata.getId().toString();
        }

        GraphMetadata parentGraphMetadata = graphMetadata.getParentGraph();
        if (parentGraphMetadata != null) {
            this.parentGraphId = parentGraphMetadata.getId().toString();
        }
    }

    public String get_id() {
        return _id;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getParentGraphId() {
        return parentGraphId;
    }
}
