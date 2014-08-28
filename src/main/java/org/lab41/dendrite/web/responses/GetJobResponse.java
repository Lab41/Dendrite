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

import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class GetJobResponse {
    private final String _id;
    private final String creationTime;
    private final String doneTime;
    private final String name;
    private final String projectId;
    private final String parentJobId;
    private final String state;
    private final float progress;
    private final String msg;
    private final String mapreduceJobId;

    public GetJobResponse(JobMetadata jobMetadata) {
        this._id = jobMetadata.getId().toString();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date creationTime = jobMetadata.getCreationTime();
        if (creationTime == null) {
            this.creationTime = null;
        } else {
            this.creationTime = df.format(creationTime);
        }

        Date doneTime = jobMetadata.getCreationTime();
        if (doneTime == null) {
            this.doneTime = null;
        } else {
            this.doneTime = df.format(doneTime);
        }

        ProjectMetadata projectMetadata = jobMetadata.getProject();
        if (projectMetadata == null) {
            this.projectId = null;
        } else {
            this.projectId = projectMetadata.getId().toString();
        }

        JobMetadata parentJobMetadata = jobMetadata.getParentJob();
        if (parentJobMetadata == null) {
            this.parentJobId = null;
        } else {
            this.parentJobId = parentJobMetadata.getId().toString();
        }

        this.name = jobMetadata.getName();
        this.state = jobMetadata.getState();
        this.progress = jobMetadata.getProgress();
        this.msg = jobMetadata.getMessage();
        this.mapreduceJobId = jobMetadata.getMapreduceJobId();
    }

    public String get_id() {
        return _id;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getParentJobId() {
        return parentJobId;
    }

    public String getDoneTime() {
        return doneTime;
    }

    public String getName() {
        return name;
    }

    public float getProgress() {
        return progress;
    }

    public String getState() {
        return state;
    }

    public String getMsg() {
        return msg;
    }

    public String getMapreduceJobId() {
        return mapreduceJobId;
    }
}
