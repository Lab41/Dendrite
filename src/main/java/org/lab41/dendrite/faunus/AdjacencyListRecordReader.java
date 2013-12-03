package org.lab41.dendrite.faunus;

import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.formats.VertexQueryFilter;
import com.thinkaurelius.faunus.mapreduce.FaunusCompiler;
import com.tinkerpop.blueprints.Direction;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;

import java.io.IOException;

public class AdjacencyListRecordReader extends RecordReader<NullWritable, FaunusVertex> {

    private boolean pathEnabled;
    private final LineRecordReader lineRecordReader;
    private final VertexQueryFilter vertexQuery;
    private String label;
    private FaunusVertex vertex = null;

    public AdjacencyListRecordReader(VertexQueryFilter vertexQuery, String label) {
        this.lineRecordReader = new LineRecordReader();
        this.vertexQuery = vertexQuery;
        this.label = label;
    }

    @Override
    public void initialize(final InputSplit genericSplit, final TaskAttemptContext context) throws IOException {
        this.lineRecordReader.initialize(genericSplit, context);
        this.pathEnabled = context.getConfiguration().getBoolean(FaunusCompiler.PATH_ENABLED, false);
    }

    @Override
    public boolean nextKeyValue() throws IOException {
        if (!this.lineRecordReader.nextKeyValue()) {
            return false;
        }

        this.vertex = parse(this.lineRecordReader.getCurrentValue().toString());
        this.vertexQuery.defaultFilter(this.vertex);
        this.vertex.enablePath(this.pathEnabled);
        return true;
    }

    FaunusVertex parse(String line) throws IOException {
        String[] parts = line.split(" ");

        if (parts.length < 2) {
            throw new IOException("Invalid line");
        }

        FaunusVertex vertex = new FaunusVertex(Long.parseLong(parts[0]));

        long count = Long.parseLong(parts[1]);

        for (int i = 0; i < count; ++i) {
            vertex.addEdge(Direction.OUT, label, Long.parseLong(parts[2 + i]));
        }

        return vertex;
    }

    @Override
    public NullWritable getCurrentKey() {
        return NullWritable.get();
    }

    @Override
    public FaunusVertex getCurrentValue() {
        return this.vertex;
    }

    @Override
    public float getProgress() throws IOException {
        return this.lineRecordReader.getProgress();
    }

    @Override
    public synchronized void close() throws IOException {
        this.lineRecordReader.close();
    }
}
