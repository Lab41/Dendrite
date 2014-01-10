package org.lab41.dendrite.metagraph;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lab41.dendrite.metagraph.models.BranchMetadata;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

import static org.junit.matchers.JUnitMatchers.hasItem;

public class ProjectMetadataTest extends BaseMetadataTest {

    ProjectMetadata projectMetadata;

    @Before
    public void setUp() {
        projectMetadata = tx.createProject("test");
    }

    @After
    public void tearDown() {
        projectMetadata = null;
    }

    @Test
    public void typeIsCorrect() {
        Assert.assertEquals(projectMetadata.asVertex().getProperty("type"), "project");
    }

    @Test
    public void projectsShouldMakeAJob() {
        // Make sure the job is linked to the project.
        JobMetadata jobMetadata = tx.createJob(projectMetadata);
        Assert.assertNotNull(jobMetadata);
        Assert.assertEquals(jobMetadata.getProject(), projectMetadata);
        Assert.assertThat(projectMetadata.getJobs(), hasItem(jobMetadata));

        // Make sure the job links are empty.
        Assert.assertNull(jobMetadata.getParentJob());
        Assert.assertEquals(jobMetadata.getChildJobs().iterator().hasNext(), false);
    }

    @Test
    public void projectsShouldMakeABranch() {
        BranchMetadata branchMetadata = tx.createBranch("test", projectMetadata);
        Assert.assertNotNull(branchMetadata);
        Assert.assertEquals(branchMetadata.getProject(), projectMetadata);
        Assert.assertThat(projectMetadata.getBranches(), hasItem(branchMetadata));

        GraphMetadata graphMetadata = branchMetadata.getGraph();
        Assert.assertNotNull(graphMetadata);
        Assert.assertNotSame(graphMetadata, projectMetadata.getGraphs());
        Assert.assertThat(projectMetadata.getGraphs(), hasItem(graphMetadata));
    }

    @Test
    public void projectsShouldSupportChangingBranches() {
        BranchMetadata branchMetadata = tx.createBranch("test", projectMetadata);
        GraphMetadata graphMetadata = branchMetadata.getGraph();

        projectMetadata.setCurrentBranch(branchMetadata);

        Assert.assertEquals(projectMetadata.getCurrentGraph(), graphMetadata);
    }

    @Test
    public void projectsShouldGetBranchesByName() {
        BranchMetadata masterBranch = projectMetadata.getCurrentBranch();
        Assert.assertEquals(masterBranch, projectMetadata.getBranchByName("master"));

        BranchMetadata testBranch = tx.createBranch("test", projectMetadata);
        Assert.assertEquals(testBranch, projectMetadata.getBranchByName("test"));

        Assert.assertNull(projectMetadata.getBranchByName("foo"));
    }
}
