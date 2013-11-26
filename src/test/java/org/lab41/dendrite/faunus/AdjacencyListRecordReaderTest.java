package org.lab41.dendrite.faunus;

import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.formats.VertexQueryFilter;
import com.thinkaurelius.faunus.mapreduce.util.EmptyConfiguration;
import com.tinkerpop.blueprints.Direction;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AdjacencyListRecordReaderTest {

    private static String LABEL = "label";

    public static <T> ArrayList<T> asList(final Iterable<T> iterable) {
        final ArrayList<T> list = new ArrayList<T>();
        for (final T t: iterable) {
            list.add(t);
        }
        return list;
    }

    @Test
    public void testRecordReader() throws Exception {
        FileSplit split = new FileSplit(
                new Path(AdjacencyListInputFormatTest.class.getResource("graph-of-the-gods.adj").toURI()),
                0,
                Long.MAX_VALUE,
                new String[]{});
        TaskAttemptContext context = new TaskAttemptContextImpl(new Configuration(), new TaskAttemptID());
        AdjacencyListRecordReader reader = new AdjacencyListRecordReader(
                VertexQueryFilter.create(new EmptyConfiguration()),
                LABEL);
        Map<Long, FaunusVertex> graph = new HashMap<Long, FaunusVertex>();

        reader.initialize(split, context);

        while (reader.nextKeyValue()) {
            assertEquals(reader.getCurrentKey(), NullWritable.get());
            FaunusVertex vertex = reader.getCurrentValue();
            graph.put(vertex.getIdAsLong(), vertex);
        }

        reader.close();

        FaunusVertex v0 = graph.get(0L);
        FaunusVertex v1 = graph.get(1L);
        FaunusVertex v2 = graph.get(2L);
        FaunusVertex v3 = graph.get(3L);
        FaunusVertex v4 = graph.get(4L);
        FaunusVertex v5 = graph.get(5L);
        FaunusVertex v6 = graph.get(6L);
        FaunusVertex v7 = graph.get(7L);
        FaunusVertex v8 = graph.get(8L);
        FaunusVertex v9 = graph.get(9L);
        FaunusVertex v10 = graph.get(10L);
        FaunusVertex v11 = graph.get(11L);

        assertEquals(asList(v0.getVertices(Direction.OUT, LABEL)), Arrays.asList());
        assertEquals(asList(v1.getVertices(Direction.OUT, LABEL)), Arrays.asList(v0, v2, v3, v4));
        assertEquals(asList(v2.getVertices(Direction.OUT, LABEL)), Arrays.asList(v1, v3, v5));
        assertEquals(asList(v3.getVertices(Direction.OUT, LABEL)), Arrays.asList(v1, v2, v6, v11));
        assertEquals(asList(v4.getVertices(Direction.OUT, LABEL)), Arrays.asList());
        assertEquals(asList(v5.getVertices(Direction.OUT, LABEL)), Arrays.asList());
        assertEquals(asList(v6.getVertices(Direction.OUT, LABEL)), Arrays.asList());
        assertEquals(asList(v7.getVertices(Direction.OUT, LABEL)), Arrays.asList(v1, v8, v9, v10, v11));
        assertEquals(asList(v8.getVertices(Direction.OUT, LABEL)), Arrays.asList());
        assertEquals(asList(v9.getVertices(Direction.OUT, LABEL)), Arrays.asList());
        assertEquals(asList(v10.getVertices(Direction.OUT, LABEL)), Arrays.asList());
        assertEquals(asList(v11.getVertices(Direction.OUT, LABEL)), Arrays.asList(v6));
    }
}
