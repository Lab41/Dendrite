package org.lab41.dendrite.services.analysis.jung;

import org.lab41.dendrite.jobs.jung.ClosenessCentralityJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.services.analysis.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ClosenessCentralityService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(ClosenessCentralityService.class);

    @Autowired
    MetaGraphService metaGraphService;

    @Async
    public void run(DendriteGraph graph, JobMetadata.Id jobId) {
        ClosenessCentralityJob job = new ClosenessCentralityJob(
                metaGraphService.getMetaGraph(),
                jobId,
                graph);

        job.run();
    }
}
