package org.lab41.dendrite.metagraph;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;

public class BaseMetaGraphTest {

    protected MetaGraph metaGraph;

    @Before
    public void setUp() {
        Configuration config = new BaseConfiguration();

        config.setProperty("metagraph.system.storage.backend", "inmemory");
        config.setProperty("metagraph.template.storage.backend", "inmemory");

        metaGraph = new MetaGraph(config);
    }

    @After
    public void tearDown() {
        metaGraph.stop();
        metaGraph = null;
    }
}
