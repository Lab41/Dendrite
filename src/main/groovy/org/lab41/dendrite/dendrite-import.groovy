import com.thinkaurelius.faunus.FaunusVertex
import com.tinkerpop.blueprints.Graph
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.util.wrappers.id.IdGraph
import com.thinkaurelius.titan.graphdb.blueprints.TitanBlueprintsGraph
import com.thinkaurelius.titan.graphdb.database.StandardTitanGraph
import org.apache.hadoop.mapreduce.Mapper

import static com.thinkaurelius.faunus.formats.BlueprintsGraphOutputMapReduce.Counters.*
import static com.thinkaurelius.faunus.formats.BlueprintsGraphOutputMapReduce.LOGGER

def Vertex getOrCreateVertex(final FaunusVertex faunusVertex, final Graph graph, final Mapper.Context context) {
    Long id = faunusVertex.getIdAsLong();
    Vertex blueprintsVertex;

    try {
        blueprintsVertex = graph.getVertex(id);

        if (null == blueprintsVertex) {
            throw new RuntimeException("The vertex does not exist in Titan: " + faunusVertex);
        } else {
            context.getCounter(VERTICES_RETRIEVED).increment(1l);
            LOGGER.error("The titan vertex is found as: " + blueprintsVertex);
        }

        for (final String property : faunusVertex.getPropertyKeys()) {
            blueprintsVertex.setProperty(property, faunusVertex.getProperty(property));
            context.getCounter(VERTEX_PROPERTIES_WRITTEN).increment(1l);
        }

        LOGGER.error("The blueprints vertex is: " + blueprintsVertex);
    } catch(Exception e) {
        e.printStackTrace();
        throw e;
    }

    return blueprintsVertex;
}
