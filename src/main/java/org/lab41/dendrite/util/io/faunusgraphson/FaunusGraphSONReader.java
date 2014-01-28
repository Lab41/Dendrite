/**
 * Copyright 2014
  In-Q-Tel/Lab41
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

package org.lab41.dendrite.util.io.faunusgraphson;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph;

import com.thinkaurelius.faunus.FaunusVertex;
import com.thinkaurelius.faunus.FaunusEdge;
import com.thinkaurelius.faunus.formats.graphson.FaunusGraphSONUtility;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * FaunusGraphSONReader reads the data from a Faunus GraphSON stream to a graph.
 */
public class FaunusGraphSONReader {
    private final Graph graph;

    /**
     * @param graph the graph to populate with the JSON data
     */
    public FaunusGraphSONReader(final Graph graph) {
        this.graph = graph;
    }

    /**
     * Input the JSON stream data into the graph.
     * In practice, usually the provided graph is empty.
     *
     * @param inputStream an InputStream of JSON data
     * @throws IOException thrown when the JSON data is not correctly formatted
     */
    public void inputGraph(final InputStream inputStream) throws IOException {
        FaunusGraphSONReader.inputGraph(this.graph, inputStream, 1000);
    }

    /**
     * Input the JSON stream data into the graph.
     * In practice, usually the provided graph is empty.
     *
     * @param inputStream an InputStream of JSON data
     * @param graph       the graph to populate with the JSON data
     * @throws IOException thrown when the JSON data is not correctly formatted
     */
    public static void inputGraph(final Graph inputGraph, final InputStream inputStream) throws IOException {
        FaunusGraphSONReader.inputGraph(inputGraph, inputStream, 1000);
    }


    /**
     * Input the JSON stream data into the graph.
     * In practice, usually the provided graph is empty.
     *
     * @param inputStream an InputStream of JSON data
     * @param graph       the graph to populate with the JSON data
     * @param bufferSize  the amount of elements to hold in memory before committing a transactions (only valid for TransactionalGraphs)
     * @throws IOException thrown when the JSON data is not correctly formatted
     */
    public static void inputGraph(final Graph inputGraph, final InputStream inputStream, int bufferSize) throws IOException {
      // titan requires creation of vertices before edges
      // unless enabling the option to set vertex id upon creation,
      // use a map to store the idFaunus->idBlueprints transformation
      FaunusEdge fe;
      Vertex vertexIn, vertexOut;
      long faunusIdIn, faunusIdOut, blueprintsIdIn, blueprintsIdOut;
      HashMap<Long, Long> faunusToBlueprintsId = new HashMap<Long, Long>();

      // if this is a transactional graph then we're buffering
      final BatchGraph graph = BatchGraph.wrap(inputGraph, bufferSize);

      // load list of vertices
      List<FaunusVertex> faunusVertexList = FaunusGraphSONUtility.fromJSON(inputStream);

      // add vertices w/ properties to graph, also saving id->id mapping for edge creation
      Vertex blueprintsVertex;
      for (FaunusVertex faunusVertex : faunusVertexList) {
        blueprintsVertex = graph.addVertex(faunusVertex.getIdAsLong());
        for (String property : faunusVertex.getPropertyKeys()) {
          blueprintsVertex.setProperty(property, faunusVertex.getProperty(property));
        }
        faunusToBlueprintsId.put(faunusVertex.getIdAsLong(), (Long) blueprintsVertex.getId());
      }

      // add edges between vertices
      for (FaunusVertex faunusVertex : faunusVertexList) {
        for (Edge edge : faunusVertex.getEdges(Direction.BOTH)) {
            fe = (FaunusEdge) edge;

            // retrieve the vertices stored in the graph
            faunusIdIn = fe.getVertexId(Direction.IN);
            blueprintsIdIn = faunusToBlueprintsId.get(faunusIdIn);
            faunusIdOut = fe.getVertexId(Direction.OUT);
            blueprintsIdOut = faunusToBlueprintsId.get(faunusIdOut);
            vertexIn = graph.getVertex(blueprintsIdIn);
            vertexOut = graph.getVertex(blueprintsIdOut);

            // save the edge to the graph
            graph.addEdge(null, vertexIn, vertexOut, fe.getLabel());
        }
      }

      // commit changes to the graph
      graph.commit();
    }

}
