package org.lab41.dendrite.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.MetaGraphTransactionBuilder;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Service
public class MetaGraphService {

    static Logger logger = LoggerFactory.getLogger(MetaGraphService.class);

    private MetaGraph metaGraph;

    @Autowired(required = true)
    public MetaGraphService(@Value("${metagraph.properties}") String pathToProperties, ResourceLoader resourceLoader) throws IOException, ConfigurationException {

        Resource resource = resourceLoader.getResource(pathToProperties);
        Configuration config = new PropertiesConfiguration(resource.getFile());

        this.metaGraph = new MetaGraph(config);
    }

    public MetaGraph getMetaGraph() {
        return metaGraph;
    }

    public Set<String> getGraphNames() {
        return metaGraph.getGraphNames();
    }

    public Collection<DendriteGraph> getGraphs() {
        return metaGraph.getGraphs();
    }

    public Collection<DendriteGraph> getGraphs(boolean includeSystemGraphs) {
        return metaGraph.getGraphs(includeSystemGraphs);
    }

    public DendriteGraph getGraph(String id) {
        return metaGraph.getGraph(id);
    }

    public MetaGraphTx newTransaction() {
        return metaGraph.newTransaction();
    }

    public MetaGraphTransactionBuilder buildTransaction() {
        return metaGraph.buildTransaction();
    }

    public void stop() {
        metaGraph.stop();
    }
}
