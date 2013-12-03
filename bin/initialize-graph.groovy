import org.apache.commons.configuration.BaseConfiguration
import com.thinkaurelius.titan.core.TitanFactory

config = new BaseConfiguration()
config.setProperty("storage.backend", "hbase")
config.setProperty("storage.hostname", "localhost")
config.setProperty("storage.port", "2181")
config.setProperty("storage.tablename", "dendrite-hbase")

graph = TitanFactory.open(config)

graph.makeKey("name").dataType(String.class).indexed(Vertex.class).unique().make()
graph.makeKey("age").dataType(Integer.class).indexed(Vertex.class).make()
graph.makeKey("type").dataType(String.class).make()

time = graph.makeKey("time").dataType(Integer.class).make()
reason = graph.makeKey("reason").dataType(String.class).indexed(Edge.class).make()
//graph.makeKey("place").dataType(Geoshape.class).indexed(Edge.class).make()
graph.makeKey("place").dataType(String.class).indexed(Edge.class).make()

/*
graph.makeLabel("father").manyToOne().make()
graph.makeLabel("mother").manyToOne().make()
graph.makeLabel("battled").sortKey(time).make()
graph.makeLabel("lives").signature(reason).make()
graph.makeLabel("pet").make()
graph.makeLabel("brother").make()
*/

graph.makeLabel("father").make()
graph.makeLabel("mother").make()
graph.makeLabel("battled").sortKey(time).make()
graph.makeLabel("lives").signature(reason).make()
graph.makeLabel("pet").make()
graph.makeLabel("brother").make()

graph.commit()
