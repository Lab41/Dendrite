/**
 * Copyright 2013 In-Q-Tel/Lab41
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

'use strict';

/* Services */

angular.module('dendrite.services', ['ngResource']).
    // User factory to handle authentication
    factory('User', function($rootScope, $http, $location, $cookieStore) {
      var _this = this;
      this.authenticated = false;
      this.name = null;

      var accessLevels = routingConfig.accessLevels
          , userRoles = routingConfig.userRoles
          , currentUser = $cookieStore.get('user') || { username: '', role: userRoles.ROLE_PUBLIC };

      $cookieStore.remove('user');

      function changeUser(user) {
          _.extend(currentUser, user);
      }

      function ping() {
        // Check if we're already authenticated...
        $http.get('api/user').success(function(response) {
          if (response.name === "anonymous") {
            $rootScope.$emit("event:loginRequired");
          } else {
            _this.authenticated = true;
            _this.name = response.name;
            currentUser.role = userRoles[response.authorities[0].authority];
          }
        });
      }

      ping();

      return {
        isAuthenticated: function() {
          return _this.authenticated;
        },

        getName: function() {
          return _this.name;
        },

        // login posts to spring password check and authenticates or returns empty callback
        // spring expects j_username/j_password as parameters
        login: function(username, password) {
          var payload = $.param({
            j_username: username,
            j_password: password,
            _spring_security_remember_me: "on"
          });
          var config = {
            headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'}
          };

          return $http.post($rootScope.url_login, payload, config).success(function(response) {
            ping();
          });
        },

        // logout posts to spring logout URL
        // on success, broadcast to app
        logout: function() {
          return $http.post($rootScope.url_logout, {}).
            success(function(response) {
              _this.authenticated = false;
              currentUser.role = userRoles.ROLE_PUBLIC;
              $cookieStore.remove('user');
              $rootScope.$broadcast('event:logoutConfirmed');
            });
        },

        //TODO: implement SpringMVC "ping" route to return ROLE_xxx
        getRole: function() {
          /*
          $http.get('user/roles').success(function(response) {
            currentUser.role = userRoles[response.authorities[0].authority];
          });
          */
          return currentUser.role;
        },

        // resetRole reverts current user's role to public access
        resetRole: function() {
          return currentUser.role = userRoles.ROLE_ANON;
        },

        authorize: function(accessLevel, role) {
            if(role === undefined)
                role = currentUser.role;
            if(accessLevel === undefined)
                accessLevel = userRoles.ROLE_PUBLIC;
            return accessLevel.bitMask & role.bitMask;
        },

        isLoggedIn: function(user) {
            if(user === undefined)
                user = currentUser;
            return user.role == userRoles.ROLE_USER || user.role == userRoles.ROLE_ADMIN;
        },

        accessLevels: accessLevels,
        userRoles: userRoles,
        user: currentUser
      };
    }).
    factory('Helpers', function($resource) {
        //TODO: delete once backend APIs complete - dummy result array
        var analyticResults = [];

        return {
          //TODO: delete once backend APIs complete - function to add class for progress bars
          colorProgressBars: function(progress) {
            if (progress < 0.33) {
              return 'danger';
            } else if (progress < 0.66) {
              return 'warning';
            } else if (progress < 1.0) {
              return 'info';
            } else {
              return 'success';
            }
          },
          parseGraphFile: function(text, format) {
            var keys = {};
            try {
              if (format !== "Select Format") {
                if (format === "GraphSON") {

                  // parse the graph's vertices for available keys
                  var json = JSON.parse(text);
                  Array().forEach.call(json.graph.vertices, function(v) {
                      Object.keys(v).forEach(function(key) {
                        keys[key] = true;
                      });
                  });

                }
                else if (format === "GraphML") {

                  // parse the XML and extract the graph>node>data keys
                  var xml = new DOMParser().parseFromString(text, "text/xml");
                  Array().forEach.call(xml.getElementsByTagName('node'), function(node) {
                    Array().forEach.call(node.attributes, function(attribute) {
                      keys[attribute.nodeName] = true;
                    });
                    Array().forEach.call(node.getElementsByTagName('data'), function(data) {
                      keys[data.getAttribute('key')] = true;
                    });
                  });

                }
                else if (format === "GML") {
                  // TODO
                }
                else if (format === "FaunusGraphSON") {

                  // read the file line-by-line, parse into JSON, and extract the keys
                  text.split('\n').forEach(function(line) {
                    if (line.length) {
                      var json = JSON.parse(line);
                      Object.keys(json).forEach(function(key) {
                        keys[key] = true;
                      });
                    }
                  });
                }
              }
            }
            catch (err) {

            }

            return Object.keys(keys);
          }
        };
    }).
    factory('ElasticSearch', function($resource, $routeParams, $http, appConfig) {
        return {

          search: function(queryParams) {
            // build elasticSearch query
            var inputJson = {
                    "from" : queryParams.pageSize*(queryParams.pageNumber - 1),
                    "size" : queryParams.pageSize,
                    "query" : { "query_string" : {"query" : "*"+queryParams.queryTerm+"*"} },
                    "filter" : { "type": { "value": queryParams.resultType } },
                    "sort" : queryParams.sortInfo
                };

            // query server
            return $http({
                method: "POST",
                url: '/dendrite/api/graphs/'+$routeParams.graphId+'/search',
                data: JSON.stringify(inputJson)
            });

          }
        };
    }).
    factory('Histogram', function($resource, $routeParams, $http, appConfig) {
      return {
        searchFacets: function(graphId) {
          return $http({
              method: "GET",
              url: '/dendrite/api/graphs/'+graphId+'/search/mapping'
          })
        },

        display: function(graphId, queryTerm, queryFacet) {
          // default inputs
          if (queryTerm === undefined || queryTerm === '') {
            queryTerm = "*";
          }
          if (queryFacet === undefined || queryFacet === '') {
            queryFacet = "reason";
          }

          // build elasticSearch query
          var inputJson = {
                    "size" : 0,
                    "query" : { "query_string" : {"query" : queryTerm} },
                    "facets" : {
                        "tags" : { "terms": {"field": queryFacet, "size": appConfig.elasticSearch.fieldSize}
                        }
                    }
                  };

          // query server
          return $http({
              method: "POST",
              url: '/dendrite/api/graphs/'+graphId+'/search',
              data: JSON.stringify(inputJson)
          })
          .success(function(json) {
            var facets = json.facets.tags.terms;

            // helper functions to extract properties of object array
            var getCount, getTerm;
            getCount = function(d) {
              return d.count;
            };
            getTerm = function(d) {
              return d.term;
            };

            // viz properties
            var names,scores,
                x,y,height,
                chart,
                width = 450,
                bar_height = 20,
                padding_width = 40,
                padding_height = 30,
                left_width = 100,
                gap = 2;

            // extract names and scores
            names = facets.map(getTerm);
            scores = facets.map(getCount);
            height = bar_height * names.length;

            // remove existing svg on refresh
            $("#viz-histogram-wrapper svg").remove();

            // add titlebar
            $('#viz-histogram-title').html('Results for facet "'+queryFacet+'" in query "' + queryTerm+'":');

            // add canvas
            chart = d3.select("#viz-histogram-wrapper")
              .append('svg')
              .attr('class', 'chart')
              .attr('width', width)
              .attr('height', height);

            // establish scales for x,y
            x = d3.scale.linear()
               .domain([0, d3.max(scores)])
               .range([0, width]);

            // redefine y for adjusting the gap
            y = d3.scale.ordinal()
              .domain(names)
              .rangeBands([0, (bar_height + 2 * gap) * names.length]);

            chart = d3.select("#viz-histogram-wrapper")
              .append('svg')
              .attr('class', 'chart')
              .attr('width', left_width + width + padding_width)
              .attr('height', (bar_height + gap * 2) * names.length + padding_height)
              .append("g")
              .attr("transform", "translate(0, 0)");

            // scale ruler lines
            chart.selectAll("line")
              .data(x.ticks(d3.max(scores)))
              .enter().append("line")
              .attr("x1", function(d) { return x(d) + left_width; })
              .attr("x2", function(d) { return x(d) + left_width; })
              .attr("y1", 0)
              .attr("y2", (bar_height + gap * 2) * names.length);

            // color bars
            chart.selectAll("rect")
              .data(facets)
              .enter().append("rect")
              .attr("x", left_width)
              .attr("y", function(d) { return y(d.term) + gap; })
              .attr("width", function(d) { return x(d.count) })
              .attr("height", bar_height);

            // display count
            chart.selectAll("text.score")
              .data(facets)
              .enter().append("text")
              .attr("x", function(d) { return x(d.count) + left_width; })
              .attr("y", function(d, i){ return y(d.term) + y.rangeBand()/2; } )
              .attr("dx", -5)
              .attr("dy", ".36em")
              .attr("text-anchor", "end")
              .attr('class', 'score')
              .text(function(d) { return d.count });

            // category label
            chart.selectAll("text.name")
              .data(facets)
              .enter().append("text")
              .attr("x", left_width / 2)
              .attr("y", function(d, i){ return y(d.term) + y.rangeBand()/2; } )
              .attr("dy", ".36em")
              .attr("text-anchor", "middle")
              .attr('class', 'name')
              .text(function(d) { return d.term });
            });
        }
      };
    }).
    factory('Analytics', function($resource, $routeParams, $http, Project, Graph, appConfig) {
        return $resource('api/projects/:projectId/jobs', {
            projectId: '@projectId'
        }, {

          getJob: {
            url: 'api/jobs/:jobId',
            method: 'GET',
            isArray: false
          },

          deleteJob: {
            url: 'api/jobs/:jobId',
            method: 'DELETE',
            isArray: false
          },

          // fire off calculation
          createPageRankJung: {
            url: 'api/graphs/:graphId/analysis/jung-pagerank',
            method: 'POST',
            isArray: false
          },

          createBetweennessCentralityJung: {
            url: 'api/graphs/:graphId/analysis/jung-betweenness-centrality',
            method: 'POST',
            isArray: false
          },

          createEdgeDegreesFaunus: {
            url: 'api/graphs/:graphId/analysis/faunus-degrees',
            method: 'POST',
            isArray: false
          },

          createEdgeDegreesTitan: {
            url: 'api/graphs/:graphId/analysis/titan-degrees',
            method: 'POST',
            isArray: false
          }
        });
    }).
    factory('Project', function($resource) {
        return $resource('api/projects/:projectId', {
            projectId: '@projectId'
        }, {
            query: {
              method: 'GET',
              isArray: false
            },
            delete: {
              method: 'DELETE'
            },
            index: {
              url: 'api/projects',
              method: 'GET',
              isArray: false
            },
            graphs: {
              url: 'api/projects/:projectId/graphs',
              method: 'GET',
              isArray: false
            },
            create: {
              url: 'api/projects',
              method: 'POST',
              isArray: false
            },
            jobs: {
              url: 'api/projects/:projectId/jobs',
              method: 'GET'
            }
        });
    }).
    factory('Graph', function($resource) {
        return $resource('rexster-resource/graphs/:graphId', {
            graphId: '@name'
        }, {
            get: {
                url: 'api/graphs/:graphId',
                method: 'GET',
                isArray: false
            },
            query: {
                method: 'GET',
                isArray: false
            },
            index: {
              url: 'api/graphs',
              method: 'GET',
              isArray: false
            }
        });
    }).
    factory('GraphTransform', function($resource, $rootScope, $http, $q, Vertex, Edge) {
        return {
          saveFile: function(graphId, outputFormat) {
            var payload = $.param({
              format: outputFormat
            });
            var config = {
              headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'}
            };

            return $http.post('api/graphs/'+graphId+'/file-save', payload, config);
          },
          reloadGraph: function(graphId) {
            var forceDirectedGraphData = {
              vertices: $q.defer(),
              edges: $q.defer(),
            };

            Vertex.query({graphId: graphId}, function(vertices) {
              forceDirectedGraphData.vertices.resolve(vertices);
            }, function() {
              forceDirectedGraphData.vertices.reject();
            });

            Edge.query({graphId: graphId}, function(edges) {
              forceDirectedGraphData.edges.resolve(edges);
            }, function() {
              forceDirectedGraphData.edges.reject();
            });

            return forceDirectedGraphData;
          }
        };
    }).
    factory('Vertex', function($resource) {
        return $resource('rexster-resource/graphs/:graphId/vertices/:vertexId', {
            graphId: '@graphId',
            vertexId: '@_id'
        }, {
            save: {
                url: 'rexster-resource/graphs/:graphId/vertices',
                method: 'POST'
            },
            show: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId',
                method: 'GET',
                isArray: false
            },
            update: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId',
                method: 'PUT'
            },
            delete: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId',
                method: 'DELETE'
            },
            query: {
                method: 'GET',
                isArray: false
            },
            queryConnectedVertices: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/both',
                method: 'GET',
                isArray: false
            },
            queryConnectedInVertices: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/in',
                method: 'GET',
                isArray: false
            },
            queryConnectedOutVertices: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/out',
                method: 'GET',
                isArray: false
            },
            queryEdges: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/bothE',
                method: 'GET',
                isArray: false
            },
            queryInEdges: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/inE',
                method: 'GET',
                isArray: false
            },
            queryOutEdges: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/outE',
                method: 'GET',
                isArray: false
            },
            list: {
                url: 'rexster-resource/graphs/:graphId/vertices',
                method: 'GET',
                isArray: false
            }
        });
    }).
    factory('Edge', function($resource) {
        return $resource('rexster-resource/graphs/:graphId/edges/:edgeId', {
            graphId: '@graphId',
            edgeId: '@_id'
        }, {
            save: {
                method: 'POST'
            },
            update: {
                url: 'rexster-resource/graphs/:graphId/edges/:edgeId',
                method: 'PUT'
            },
            delete: {
                url: 'rexster-resource/graphs/:graphId/edges/:edgeId',
                method: 'DELETE'
            },
            query: {
                method: 'GET',
                isArray: false
            }
        });
    });
