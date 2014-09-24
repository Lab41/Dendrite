package org.lab41.dendrite.services.analysis;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.lab41.dendrite.jobs.snap.CentralityJob;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
public class SnapService extends AnalysisService {

    Logger logger = LoggerFactory.getLogger(SnapService.class);

    @Value("${snap.properties}")
    String pathToProperties;

    @Autowired
    ResourceLoader resourceLoader;

    @Async
    public void snapCentrality(DendriteGraph graph, JobMetadata.Id jobId) throws Exception {
        // load configuration
        Resource resource = resourceLoader.getResource(pathToProperties);
        Configuration config = new PropertiesConfiguration(resource.getFile());

        Path pathToSnap = Paths.get(config.getString("metagraph.template.snap.algorithm-path"));

        CentralityJob job = new CentralityJob(
                metaGraphService.getMetaGraph(),
                jobId,
                graph,
                pathToSnap);

        job.call();
    }



    }
}
