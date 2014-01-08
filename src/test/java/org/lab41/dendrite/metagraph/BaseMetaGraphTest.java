package org.lab41.dendrite.metagraph;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;

public class BaseMetaGraphTest {

    protected MetaGraph metaGraph;

    @Before
    public void setUpBaseMetaGraphTest() {
        Configuration config = new BaseConfiguration();

        config.setProperty("metagraph.storage.backend", "inmemory");

        metaGraph = new MetaGraph(config);
    }

    @After
    public void tearDownBaseMetaGraphTest() {
        metaGraph.stop();
    }
}
