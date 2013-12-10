g.makeKey("name").dataType(String.class).indexed(Vertex.class).unique().make()
g.makeKey("age").dataType(Integer.class).indexed(Vertex.class).make()
g.makeKey("type").dataType(String.class).make()

time = g.makeKey("time").dataType(Integer.class).make()
reason = g.makeKey("reason").dataType(String.class).indexed(Edge.class).make()
//g.makeKey("place").dataType(Geoshape.class).indexed(Edge.class).make()
g.makeKey("place").dataType(String.class).indexed(Edge.class).make()

/*
g.makeLabel("father").manyToOne().make()
g.makeLabel("mother").manyToOne().make()
g.makeLabel("battled").sortKey(time).make()
g.makeLabel("lives").signature(reason).make()
g.makeLabel("pet").make()
g.makeLabel("brother").make()
*/

g.makeLabel("father").make()
g.makeLabel("mother").make()
g.makeLabel("battled").sortKey(time).make()
g.makeLabel("lives").signature(reason).make()
g.makeLabel("pet").make()
g.makeLabel("brother").make()

g.commit()
