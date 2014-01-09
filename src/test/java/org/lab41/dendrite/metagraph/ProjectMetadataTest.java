package org.lab41.dendrite.metagraph;

import org.junit.Assert;
import org.junit.Test;
import org.lab41.dendrite.metagraph.models.GraphMetadata;
import org.lab41.dendrite.metagraph.models.JobMetadata;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;

import static org.junit.matchers.JUnitMatchers.hasItem;

public class ProjectMetadataTest extends BaseMetadataTest {

    @Test(expected = IllegalArgumentException.class)
    public void createProjectShouldThrowWithEmptyName() {
        tx.createProject("");
    }

    @Test
    public void createProjectShouldMakeAProject() {
        ProjectMetadata projectMetadata = tx.createProject("test");
        Assert.assertNotNull(projectMetadata);
        Assert.assertEquals(projectMetadata.getName(), "test");
    }

    @Test
    public void createProjectShouldMakeADefaultGraph() {
        ProjectMetadata projectMetadata = tx.createProject("test");

        // Make sure the graph is linked to the project.
        GraphMetadata graphMetadata = projectMetadata.getCurrentGraph();
        Assert.assertNotNull(graphMetadata);
        Assert.assertEquals(graphMetadata.getProject(), projectMetadata);
        Assert.assertThat(projectMetadata.getGraphs(), hasItem(graphMetadata));

        // Make sure the graph links are empty.
        Assert.assertNull(graphMetadata.getParentGraph());
        Assert.assertEquals(graphMetadata.getChildGraphs().iterator().hasNext(), false);
    }

    @Test
    public void projectsShouldMakeAJob() {
        ProjectMetadata projectMetadata = tx.createProject("test");

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
    public void parentJobsShouldMakeChildJobs() {
        ProjectMetadata projectMetadata = tx.createProject("test");

        JobMetadata parentJobMetadata = tx.createJob(projectMetadata);
        Assert.assertNotNull(parentJobMetadata);

        JobMetadata childJobMetadata = tx.createJob(parentJobMetadata);
        Assert.assertNotNull(childJobMetadata);
        Assert.assertEquals(childJobMetadata.getParentJob(), parentJobMetadata);
        Assert.assertThat(parentJobMetadata.getChildJobs(), hasItem(childJobMetadata));

        // Make sure the project links are created.
        Assert.assertThat(projectMetadata.getJobs(), hasItem(childJobMetadata));
        Assert.assertEquals(childJobMetadata.getProject(), projectMetadata);
    }

}
