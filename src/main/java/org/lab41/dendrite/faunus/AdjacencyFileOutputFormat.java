package org.lab41.dendrite.faunus;

import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.formats.FaunusFileOutputFormat;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

public class AdjacencyFileOutputFormat extends FaunusFileOutputFormat {

    @Override
    public RecordWriter<NullWritable, FaunusVertex> getRecordWriter(final TaskAttemptContext job)
        throws IOException, InterruptedException {

        return new AdjacencyListRecordWriter(super.getDataOuputStream(job));
    }

}
