package org.lab41.faunus;

import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.formats.VertexQueryFilter;
import com.thinkaurelius.faunus.mapreduce.util.EmptyConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AdjacencyListRecordWriterTest {

    private static String LABEL = "label";

    @Test
    public void testRecordWriter() throws Exception {
        FileSplit split = new FileSplit(
                new Path(AdjacencyListInputFormatTest.class.getResource("graph-of-the-gods.adj").toURI()),
                0,
                Long.MAX_VALUE,
                new String[]{});
        TaskAttemptContext context = new TaskAttemptContextImpl(new Configuration(), new TaskAttemptID());
        AdjacencyListRecordReader reader = new AdjacencyListRecordReader(
                VertexQueryFilter.create(new EmptyConfiguration()),
                LABEL);
        reader.initialize(split, context);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AdjacencyListRecordWriter writer = new AdjacencyListRecordWriter(new DataOutputStream(out));

        while (reader.nextKeyValue()) {
            FaunusVertex vertex = reader.getCurrentValue();
            writer.write(NullWritable.get(), vertex);
        }
        writer.close(context);

        String received = out.toString();
        String expected = IOUtils.toString(AdjacencyListRecordWriterTest.class.getResourceAsStream("graph-of-the-gods.adj"));

        assertEquals(received, expected);
    }
}
