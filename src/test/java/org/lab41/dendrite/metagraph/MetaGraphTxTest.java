package org.lab41.dendrite.metagraph;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

import static org.junit.matchers.JUnitMatchers.hasItem;

public class MetaGraphTxTest extends BaseMetaGraphTest {
    MetaGraphTx tx;

    @Before
    public void setUp() {
        super.setUp();
        tx = metaGraph.newTransaction();
    }

    @After
    public void tearDown() {
        tx.rollback();
        tx = null;
        super.tearDown();
    }

    @Test(expected = IllegalArgumentException.class)
    public void createProjectShouldThrowWithEmptyName() {
        tx.createProject("");
    }

    @Test
    public void createProjectShouldMakeAProject() {
        ProjectMetadata projectMetadata = tx.createProject("test");
        Assert.assertNotNull(projectMetadata);
        Assert.assertEquals(projectMetadata.getName(), "test");

        // Make sure the branch is linked to the project.
        BranchMetadata branchMetadata = projectMetadata.getCurrentBranch();
        Assert.assertNotNull(branchMetadata);
        Assert.assertEquals(branchMetadata.getName(), "master");
        Assert.assertEquals(branchMetadata.getProject(), projectMetadata);
        Assert.assertThat(projectMetadata.getBranches(), hasItem(branchMetadata));

        // Make sure the graph is linked to the project.
        GraphMetadata graphMetadata = branchMetadata.getGraph();
        Assert.assertNotNull(graphMetadata);
        Assert.assertEquals(graphMetadata.getProject(), projectMetadata);
        Assert.assertThat(projectMetadata.getGraphs(), hasItem(graphMetadata));

        // Make sure the graph links are empty.
        Assert.assertNull(graphMetadata.getParentGraph());
        Assert.assertEquals(graphMetadata.getChildGraphs().iterator().hasNext(), false);
    }

    @Test
    public void createProjectShouldOptionallyNotMakeABranch() {
        ProjectMetadata projectMetadata = tx.createProject("test", false);
        Assert.assertNotNull(projectMetadata);
        Assert.assertNull(projectMetadata.getCurrentBranch());
        Assert.assertNull(projectMetadata.getCurrentGraph());
    }
}
