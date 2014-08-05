package org.lab41.dendrite.metagraph.models;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.matchers.JUnitMatchers.hasItem;

public class JobMetadataTest extends BaseMetadataTest {

    UserMetadata userMetadata;
    ProjectMetadata projectMetadata;
    JobMetadata jobMetadata;

    @Before
    public void setUp() {
        super.setUp();
        userMetadata = tx.createUser("test");
        projectMetadata = tx.createProject("test", userMetadata);
        jobMetadata = tx.createJob(projectMetadata);
    }

    @After
    public void tearDown() {
        jobMetadata = null;
        projectMetadata = null;
        super.tearDown();
    }

    @Test
    public void typeIsCorrect() {
        Assert.assertEquals(jobMetadata.asVertex().getProperty("type"), "job");
    }

    @Test
    public void parentJobsShouldMakeChildJobs() {
        JobMetadata childJobMetadata = tx.createJob(jobMetadata);
        Assert.assertNotNull(childJobMetadata);
        Assert.assertEquals(childJobMetadata.getParentJob(), jobMetadata);
        Assert.assertThat(jobMetadata.getChildJobs(), hasItem(childJobMetadata));

        // Make sure the project links are created.
        Assert.assertThat(projectMetadata.getJobs(), hasItem(childJobMetadata));
        Assert.assertEquals(childJobMetadata.getProject(), projectMetadata);
    }

    @Test
    public void timesAreSet() {
        Assert.assertNotNull(jobMetadata.getCreationTime());
        Assert.assertNull(jobMetadata.getDoneTime());

        jobMetadata.setState(JobMetadata.DONE);

        Assert.assertNotNull(jobMetadata.getDoneTime());
    }
}
