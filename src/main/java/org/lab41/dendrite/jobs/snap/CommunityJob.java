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

package org.lab41.dendrite.jobs.snap;

import com.thinkaurelius.titan.core.attribute.FullDouble;
import com.tinkerpop.blueprints.Vertex;
import org.lab41.dendrite.metagraph.DendriteGraph;
import org.lab41.dendrite.metagraph.MetaGraph;
import org.lab41.dendrite.metagraph.models.JobMetadata;

import java.nio.file.Path;

public class CommunityJob extends AbstractSnapJob {

    private static final String COMMUNITY_KEY = "snapCommunity";

    private final Path pathToExecutable;

    public CommunityJob(MetaGraph metaGraph,
                        JobMetadata.Id jobId,
                        DendriteGraph graph,
                        Path pathToSnap) {
        super(metaGraph, jobId, graph);

        this.pathToExecutable = pathToSnap.resolve("community").resolve("community");
    }

    @Override
    protected String getSnapCommand(Path inputFile, Path outputFile) {
        return pathToExecutable.toString() +
                " -i:" + inputFile +
                " -o:" + outputFile +
                " -a:1";
    }

    @Override
    protected void importLine(String[] parts) {
        String id = parts[0];
        double community = Double.valueOf(parts[1]);

        // feed snap output as input for updating each vertex
        Vertex vertex = graph.getVertex(id);
        vertex.setProperty(COMMUNITY_KEY, community);
    }

    @Override
    protected void createIndices() {
        createVertexIndex(COMMUNITY_KEY, Integer.class);
    }
}
