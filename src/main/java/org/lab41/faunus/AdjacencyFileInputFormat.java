package org.lab41.faunus;

import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.formats.VertexQueryFilter;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import java.io.IOException;

public class AdjacencyFileInputFormat extends FileInputFormat<NullWritable, FaunusVertex> implements Configurable {

    public static final String FAUNUS_GRAPH_INPUT_LABEL = "faunus.graph.input.label";

    private VertexQueryFilter vertexQuery;
    private String label;
    private Configuration config;

    @Override
    public RecordReader<NullWritable, FaunusVertex> createRecordReader(InputSplit split, TaskAttemptContext context) {
        return new AdjacencyListRecordReader(this.vertexQuery, this.label);
    }

    @Override
    protected boolean isSplitable(final JobContext context, final Path file) {
        return null == new CompressionCodecFactory(context.getConfiguration()).getCodec(file);
    }

    @Override
    public void setConf(Configuration config) {
        this.config = config;
        this.vertexQuery = VertexQueryFilter.create(config);
        this.label = config.get(FAUNUS_GRAPH_INPUT_LABEL);
    }

    @Override
    public Configuration getConf() {
        return this.config;
    }
}
