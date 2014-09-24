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

public class CentralityJob extends AbstractSnapJob {

    private static final String DEGREE_KEY = "snapDegrees";
    private static final String CLOSENESS_KEY = "snapCloseness";
    private static final String BETWEENNESS_KEY = "snapBetweenness";
    private static final String EIGENVECTOR_KEY = "snapEigenvector";
    private static final String NETWORK_CONSTRAINT_KEY = "snapNetworkConstraint";
    private static final String CLUSTERING_COEFFICIENT_KEY = "snapClusteringCoefficient";
    private static final String PAGERANK_KEY = "snapPageRank";
    private static final String HUB_SCORE_KEY = "snapHupScore";
    private static final String AUTHORITY_SCORE_KEY = "snapAuthorityScore";

    private final Path pathToExecutable;

    public CentralityJob(MetaGraph metaGraph,
                         JobMetadata.Id jobId,
                         DendriteGraph graph,
                         Path pathToSnap) {
        super(metaGraph, jobId, graph);

        this.pathToExecutable = pathToSnap.resolve("centrality").resolve("centrality");
    }

    @Override
    protected String getSnapCommand(Path inputFile, Path outputFile) {
        return pathToExecutable.toString() +
                " -i:" + inputFile +
                " -o:" + outputFile;
    }

    @Override
    protected void importLine(String[] parts) {
        String id = parts[0];
        double degree = Double.valueOf(parts[1]);
        double closeness = Double.valueOf(parts[2]);
        double betweenness = Double.valueOf(parts[3]);
        double eigenvector = Double.valueOf(parts[4]);
        double networkConstraint = Double.valueOf(parts[5]);
        double clusteringCoefficient = Double.valueOf(parts[6]);
        double pagerank = Double.valueOf(parts[7]);
        double hubScore = Double.valueOf(parts[8]);
        double authorityScore = Double.valueOf(parts[9]);

        // feed snap output as input for updating each vertex
        Vertex vertex = graph.getVertex(id);
        vertex.setProperty(DEGREE_KEY, degree);
        vertex.setProperty(CLOSENESS_KEY, closeness);
        vertex.setProperty(BETWEENNESS_KEY, betweenness);
        vertex.setProperty(EIGENVECTOR_KEY, eigenvector);
        vertex.setProperty(NETWORK_CONSTRAINT_KEY, networkConstraint);
        vertex.setProperty(CLUSTERING_COEFFICIENT_KEY, clusteringCoefficient);
        vertex.setProperty(PAGERANK_KEY, pagerank);
        vertex.setProperty(HUB_SCORE_KEY, hubScore);
        vertex.setProperty(AUTHORITY_SCORE_KEY, authorityScore);
    }

    @Override
    protected void createIndices() {
        createVertexIndex(DEGREE_KEY, FullDouble.class);
        createVertexIndex(CLOSENESS_KEY, FullDouble.class);
        createVertexIndex(BETWEENNESS_KEY, FullDouble.class);
        createVertexIndex(EIGENVECTOR_KEY, FullDouble.class);
        createVertexIndex(NETWORK_CONSTRAINT_KEY, FullDouble.class);
        createVertexIndex(CLUSTERING_COEFFICIENT_KEY, FullDouble.class);
        createVertexIndex(PAGERANK_KEY, FullDouble.class);
        createVertexIndex(HUB_SCORE_KEY, FullDouble.class);
        createVertexIndex(AUTHORITY_SCORE_KEY, FullDouble.class);
        createVertexIndex(DEGREE_KEY, FullDouble.class);
        createVertexIndex(DEGREE_KEY, FullDouble.class);
        createVertexIndex(DEGREE_KEY, FullDouble.class);
        createVertexIndex(DEGREE_KEY, FullDouble.class);
    }
}
