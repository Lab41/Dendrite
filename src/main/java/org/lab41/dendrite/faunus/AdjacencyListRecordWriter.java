package org.lab41.dendrite.faunus;

import com.thinkaurelius.faunus.FaunusVertex;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class AdjacencyListRecordWriter extends RecordWriter<NullWritable, FaunusVertex> {
    private static final String UTF8 = "UTF-8";
    private static final byte[] SPACE;
    private static final byte[] NEWLINE;

    protected DataOutputStream out;

    static {
        try {
            SPACE = " ".getBytes(UTF8);
            NEWLINE = "\n".getBytes(UTF8);
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalArgumentException("Can not find " + UTF8 + " encoding");
        }
    }

    public AdjacencyListRecordWriter(final DataOutputStream out) {
        this.out = out;
    }

    @Override
    public void write(final NullWritable key, final FaunusVertex vertex) throws IOException, InterruptedException {
        if (null != vertex) {
            this.out.write(Long.toString(vertex.getIdAsLong()).getBytes(UTF8));
            this.out.write(SPACE);

            List<Edge> edges = (List<Edge>) vertex.getEdges(Direction.OUT);
            this.out.write(Long.toString(edges.size()).getBytes(UTF8));

            for(Edge edge: edges) {
                final Long id = (Long) edge.getVertex(Direction.IN).getId();
                this.out.write(SPACE);
                this.out.write(id.toString().getBytes(UTF8));
            }

            this.out.write(NEWLINE);
        }
    }

    @Override
    public synchronized void close(TaskAttemptContext context) throws IOException {
        this.out.close();
    }
}
