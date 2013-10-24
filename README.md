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

```bash
git clone https://github.com/Lab4/Dendrite.git
cd Dendrite
mvn tomcat:run
```

Required Dependencies
---------------------

- Java
- Maven

Getting started
----------------

1. After the webserver is up and running browse to http://server.fqdn:8080/dendrite

2. Login as a user with user/password or as an admin with admin/password

Currently supported graph formats 
================================

- GML
- GraphML
- GraphSON

Related repositories
====================

 - [graph-generators](http://lab41.github.io/graph-generators/)
 - [MRKronecker](https://github.com/Lab41/MRKronecker)

Contributing to Dendrite
=======================

What to contribute?  Awesome!  Issue a pull request or see more details [here](https://github.com/Lab41/Dendrite/blob/master/CONTRIBUTING.md).

[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/Lab41/dendrite/trend.png)](https://bitdeli.com/free "Bitdeli Badge")
