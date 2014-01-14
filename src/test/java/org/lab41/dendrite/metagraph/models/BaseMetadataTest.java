package org.lab41.dendrite.metagraph.models;

import org.junit.After;
import org.junit.Before;
import org.lab41.dendrite.metagraph.BaseMetaGraphTest;
import org.lab41.dendrite.metagraph.MetaGraphTx;

public class BaseMetadataTest extends BaseMetaGraphTest {

    protected MetaGraphTx tx;

    @Before
    public void setUp() {
        super.setUp();
        tx = metaGraph.newTransaction();
    }

    @After
    public void tearDown() {
        tx.rollback();
        super.tearDown();
    }
}
