import com.thinkaurelius.titan.core.TitanKey
import com.thinkaurelius.titan.core.attribute.Geoshape
import com.tinkerpop.blueprints.Edge
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.util.ElementHelper

g.makeKey("vertexId").dataType(String.class).indexed(Vertex.class).indexed("search", Vertex.class).unique().make();
g.makeKey("name").dataType(String.class).indexed(Vertex.class).indexed("search", Vertex.class).unique().make();
g.makeKey("age").dataType(Integer.class).indexed("search", Vertex.class).make();
g.makeKey("type").dataType(String.class).make();

TitanKey time = g.makeKey("time").dataType(Integer.class).make();
TitanKey reason = g.makeKey("reason").dataType(String.class).indexed("search", Edge.class).make();
g.makeKey("place").dataType(Geoshape.class).indexed("search", Edge.class).make();

g.makeLabel("father").manyToOne().make();
g.makeLabel("mother").manyToOne().make();
g.makeLabel("battled").sortKey(time).make();
g.makeLabel("lives").signature(reason).make();
g.makeLabel("pet").make();
g.makeLabel("brother").make();

g.commit();

Vertex saturn = g.addVertex(null);
saturn.setProperty("name", "saturn");
saturn.setProperty("age", 10000);
saturn.setProperty("type", "titan");

Vertex sky = g.addVertex(null);
ElementHelper.setProperties(sky, "name", "sky", "type", "location", "vertexId", sky.getId().toString());

Vertex sea = g.addVertex(null);
ElementHelper.setProperties(sea, "name", "sea", "type", "location", "vertexId", sea.getId().toString());

Vertex jupiter = g.addVertex(null);
ElementHelper.setProperties(jupiter, "name", "jupiter", "age", 5000, "type", "god", "vertexId", jupiter.getId().toString());

Vertex neptune = g.addVertex(null);
ElementHelper.setProperties(neptune, "name", "neptune", "age", 4500, "type", "god", "vertexId", neptune.getId().toString());

Vertex hercules = g.addVertex(null);
ElementHelper.setProperties(hercules, "name", "hercules", "age", 30, "type", "demigod", "vertexId", hercules.getId().toString());

Vertex alcmene = g.addVertex(null);
ElementHelper.setProperties(alcmene, "name", "alcmene", "age", 45, "type", "human", "vertexId", alcmene.getId().toString());

Vertex pluto = g.addVertex(null);
ElementHelper.setProperties(pluto, "name", "pluto", "age", 4000, "type", "god", "vertexId", pluto.getId().toString());

Vertex nemean = g.addVertex(null);
ElementHelper.setProperties(nemean, "name", "nemean", "type", "monster", "vertexId", nemean.getId().toString());

Vertex hydra = g.addVertex(null);
ElementHelper.setProperties(hydra, "name", "hydra", "type", "monster", "vertexId", hydra.getId().toString());

Vertex cerberus = g.addVertex(null);
ElementHelper.setProperties(cerberus, "name", "cerberus", "type", "monster", "vertexId", cerberus.getId().toString());

Vertex tartarus = g.addVertex(null);
ElementHelper.setProperties(tartarus, "name", "tartarus", "type", "location", "vertexId", tartarus.getId().toString());

// edges

jupiter.addEdge("father", saturn);
jupiter.addEdge("lives", sky).setProperty("reason", "loves fresh breezes");
jupiter.addEdge("brother", neptune);
jupiter.addEdge("brother", pluto);

neptune.addEdge("lives", sea).setProperty("reason", "loves waves");
neptune.addEdge("brother", jupiter);
neptune.addEdge("brother", pluto);

hercules.addEdge("father", jupiter);
hercules.addEdge("mother", alcmene);
ElementHelper.setProperties(hercules.addEdge("battled", nemean), "time", 1, "place", Geoshape.point(38.1f, 23.7f));
ElementHelper.setProperties(hercules.addEdge("battled", hydra), "time", 2, "place", Geoshape.point(37.7f, 23.9f));
ElementHelper.setProperties(hercules.addEdge("battled", cerberus), "time", 12, "place", Geoshape.point(39f, 22f));

pluto.addEdge("brother", jupiter);
pluto.addEdge("brother", neptune);
pluto.addEdge("lives", tartarus).setProperty("reason", "no fear of death");
pluto.addEdge("pet", cerberus);

cerberus.addEdge("lives", tartarus);

g.commit();
