package org.lab41.dendrite.metagraph;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
}
