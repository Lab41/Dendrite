Dendrite
=======
[![Build Status](https://travis-ci.org/Lab41/Dendrite.png?branch=master)](https://travis-ci.org/Lab41/Dendrite) [![Coverage Status](https://coveralls.io/repos/Lab41/Dendrite/badge.png)](https://coveralls.io/r/Lab41/Dendrite)

People. Places. Things. Graphs.

It turns out that much of the world, both physical and virtual, can be represented as a graph. Graphs describe things that are linked together such as web pages and human societies. Like many other topics, Web technologies can make these types of powerful mathematical concepts more accessible to everyday users.
Dendrite is a Lab41 exploration of ways to analyze, manipulate, version, and share extremely large graphs:
- The Web frontend leverages AngularJS to provide a responsive data-driven experience
- The UI interacts with a backend instance of the Titan Distributed Graph Database

Install instructions
====================

1. Check out the code:

  ```
  % git clone https://github.com/Lab4/Dendrite.git
  % cd Dendrite
  ```

2. Start the application.

  Dendrite has two run profiles. The first, which is the default, is the
  development profile. This uses Titan's BerkeleyDB backend and an embedded
  Elasticsearch to service all the requests. It can be launched with:

  ```
  % ./bin/dendrite-server start
  ```

  To run Dendrite in production mode with HBase and an External Elasticsearch
  first edit ``src/main/resources/META-INF/spring/prod/dendrite.properties`` 
  to match your environment, for example:

  ```
  dendrite-graph-factory.name-prefix=dendrite-prod-
  dendrite-graph-factory.storage.backend=hbase
  dendrite-graph-factory.storage.hostname=server.fqdn
  dendrite-graph-factory.storage.port=2181
  dendrite-graph-factory.storage.index.backend=elasticsearch
  dendrite-graph-factory.storage.index.hostname=server.fqdn
  dendrite-graph-factory.storage.index.client-only=true
  dendrite-graph-factory.storage.index.local-mode=false
  metadata-graph.properties=/WEB-INF/metadata-graph.properties
  metadata.directory=/tmp/dendrite
  history.properties=/WEB-INF/history.properties
  ```

  Then launch dendrite with:

  ```
  % hadoop fs -mkdir -p dendrite/
  % hadoop fs -put src/main/groovy/org/lab41/dendrite/dendrite-import.groovy dendrite/
  % DENDRITE_PROFILE=prod ./bin/dendrite-server start
  ```

3. Once ``dendrite-server`` is running initialize a graph-of-the-gods example graph:

  ```bash
  % pip install requests
  % ./bin/dendrite create-project --script ./data/init-graph-of-the-gods.groovy my-project-name
  ```

4. After the webserver is up and running browse to http://server.fqdn:8000/dendrite
5. Login as a user with user/password or as an admin with admin/password

Required Dependencies
---------------------

- Java
- Maven
- Python
- Python Pip

Optional Dependencies
---------------------

- Hadoop
- HBase
- Elasticsearch


Currently supported import and export graph formats 
===================================================

- GML
- GraphML
- GraphSON

Related repositories
====================

 - [MRKronecker](https://github.com/Lab41/MRKronecker)
 - [graph-generators](http://lab41.github.io/graph-generators/)

Contributing to Dendrite
=======================

What to contribute?  Awesome!  Issue a pull request or see more details [here](https://github.com/Lab41/Dendrite/blob/master/CONTRIBUTING.md).

[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/Lab41/dendrite/trend.png)](https://bitdeli.com/free "Bitdeli Badge")
