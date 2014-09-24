/*
 * Copyright 2014 In-Q-Tel/Lab41
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab41.dendrite.jobs;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ExportEdgeListJob extends AbstractJob<Void> {

    private static final String UTF8 = "UTF-8";
    private static final byte[] TAB;
    private static final byte[] NEWLINE;

    static {
        try {
            TAB = "\t".getBytes(UTF8);
            NEWLINE = "\n".getBytes(UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Can not find " + UTF8 + " encoding");
        }
    }

    private final DendriteGraph graph;
    private final OutputStream outputStream;

    public ExportEdgeListJob(MetaGraph metaGraph, JobMetadata.Id jobId, DendriteGraph graph, OutputStream outputStream) {
        super(metaGraph, jobId);

        this.graph = graph;
        this.outputStream = outputStream;
    }

    @Override
    public Void call() throws Exception {

        try (BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {
            for (Edge edge : graph.getEdges()) {
                Vertex outVertex = edge.getVertex(Direction.OUT);
                Vertex inVertex = edge.getVertex(Direction.IN);
                String label = edge.getLabel();

                bos.write(outVertex.getId().toString().getBytes(UTF8));
                bos.write(TAB);
                bos.write(inVertex.getId().toString().getBytes(UTF8));
                bos.write(TAB);
                bos.write(label.getBytes(UTF8));
                bos.write(NEWLINE);
            }
        }

        return null;
    }
}
