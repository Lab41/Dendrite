package org.lab41.dendrite.models;

import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

@TypeValue("hadoop_job")
public interface HadoopJobMetadata extends JobMetadata {

    @Property("jobid")
    public String getJobId();

    @Property("jobid")
    public void setJobId(String jobId);
}
