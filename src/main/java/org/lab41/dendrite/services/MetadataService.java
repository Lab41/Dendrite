package org.lab41.dendrite.services;

import org.lab41.dendrite.graph.DendriteGraph;
import org.lab41.dendrite.graph.DendriteGraphFactory;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataService {

    static Logger logger = LoggerFactory.getLogger(MetadataService.class);

    private MetaGraph metaGraph;

    @Autowired
    public MetadataService(DendriteGraphFactory graphFactory) {
        this.metaGraph = new MetaGraph(graphFactory);
    }

    public DendriteGraph getGraph(String id) {
        return metaGraph.getGraph(id);
    }

    public MetaGraphTx newTransaction() {
        return metaGraph.newTransaction();
    }

}
