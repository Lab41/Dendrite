'use strict';

/* Directives */
angular.module('dendrite.directives', []).
  // Use: <span access-level='accessLevels.ROLE_USER'>test data</span>
  // Note: accessLevels must be set in controller
  directive('accessLevel', ['$rootScope', 'User', function($rootScope, User) {
    return {
        restrict: 'A',
        link: function($scope, element, attrs) {
            var prevDisp = element.css('display')
                , userRole
                , accessLevel;

            $scope.user = User.user;
            $scope.$watch('user', function(user) {
                if(user.role)
                    userRole = user.role;
                updateCSS();
            }, true);

            attrs.$observe('accessLevel', function(al) {
                if(al) accessLevel = $scope.$eval(al);
                updateCSS();
            });

            function updateCSS() {
                if(userRole && accessLevel) {
                    if(!User.authorize(accessLevel, userRole))
                        element.css('display', 'none');
                    else
                        element.css('display', prevDisp);
                }
            }
        }
    };
  }]).
  directive('forceDirectedGraph', ['$q', function($q) {
    return {
      restrict: 'A',
      link: function($scope, element, attrs) {
        var width = 960,
          height = 500;

        var color = d3.scale.category20();

        var force = d3.layout.force()
          .on("tick", tick)
          .charge(-120)
          .linkDistance(30)
          .size([width, height]);

        var svg = d3.select(element[0])
          .attr("width", width)
          .attr("height", height);

        var nodes = [], links = [];

        $scope.$watch(attrs.data, function(data) {
          if (data) {
            $q.all([
                data.vertices.promise,
                data.edges.promise
              ]).then(function(data) {
                var vertices = data[0].results;
                var edges = data[1].results;

                nodes = vertices.map(function(vertex) {
                  return {
                    _id: vertex._id,
                    name: vertex.name
                  };
                });

                var vertexToNode = {};
                vertices.forEach(function(vertex, idx) {
                  vertexToNode[vertex._id] = idx;
                })

                links = edges.map(function(edge) {
                  return {
                    _id: edge._id,
                    source: vertexToNode[edge._inV],
                    target: vertexToNode[edge._outV]
                  };
                });

                update();
              });
          } else {
            nodes = [];
            links = [];
            update();
          }
        });

        function update() {
          force
            .nodes(nodes)
            .links(links)
            .start();

          // Update the links.
          var link = svg.selectAll("line")
            .data(links, function(d) { return d._id; });

          // Enter any new links.
          link.enter().insert("svg:line")
            //.attr("class", "link")
            .style("stroke", "#999")
            .style("stroke-opacity", "0.6")
            .style("stroke-width", function(d) { return Math.sqrt(d.value); });

          // Exit any old links.
          link.exit().remove();

          // Update the nodes.
          var node = svg.selectAll("circle")
            .data(nodes, function(d) { return d._id; });

          // Enter any new nodes.
          node.enter().append("circle")
            //.attr("class", "node")
            .attr("r", 7)
            .style("stroke", "#fff")
            .style("stroke-width", "1.5px")
            .style("fill", function(d) { return color(d._id); })
            .call(force.drag);

          // Exit any old nodes.
          node.exit().remove();

          node.append("title")
            .text(function(d) { return d.name; });
        }

        function tick() {
          // Update the links.
          var link = svg.selectAll("line")
            .data(links, function(d) { return d._id; });

          link.attr("x1", function(d) { return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });

          // Update the nodes.
          var node = svg.selectAll("circle")
            .data(nodes, function(d) { return d._id; });

          node.attr("cx", function(d) { return d.x; })
            .attr("cy", function(d) { return d.y; });
        }
      }
    };
  }]);

 
