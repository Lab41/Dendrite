package org.lab41.dendrite.metagraph;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;

public class BaseMetadataTest extends BaseMetaGraphTest {

    protected MetaGraphTx tx;

    @Before
    public void setUpBaseMetadataTest() {
        tx = metaGraph.newTransaction();
    }

    @After
    public void tearDownBaseMetadataTest() {
        tx.rollback();
    }
}
