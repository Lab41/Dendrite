/**
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

'use strict';

// Declare app level module which depends on filters, and services
angular.module('dendrite', [
        'dendrite.filters',
        'dendrite.services',
        'dendrite.directives',
        'dendrite.controllers',
        'ngCookies',
        'ngUpload',
        'ngGrid',
        'ngSanitize',
        '$strap.directives',
        'ui.bootstrap.popover',
        'ui.bootstrap.collapse'
    ]).
  config(['$routeProvider', function($routeProvider) {
    var access = routingConfig.accessLevels;
    $routeProvider.
        when('/home', {templateUrl: 'partials/home.html', controller: 'HomeCtrl', access: access.ROLE_ANON}).
        when('/login', {templateUrl: 'partials/login.html', controller: 'LoginCtrl', access: access.ROLE_ANON}).
        when('/graphs/:graphId', {templateUrl: 'partials/graph-detail.html', controller: 'GraphDetailCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/vertices', {
          templateUrl: 'partials/vertex-list.html',
          controller: 'VertexListCtrl',
          access: access.ROLE_USER,
          reloadOnSearch: false
        }).
        when('/graphs/:graphId/vertices/:vertexId', {templateUrl: 'partials/vertex-detail.html', controller: 'VertexDetailCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/create_vertex', {templateUrl: 'partials/vertex-create.html', controller: 'VertexCreateCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/vertices/:vertexId/edit_vertex', {templateUrl: 'partials/vertex-edit.html', controller: 'VertexEditCtrl', access: access.ROLE_USER}).
        //when('/graphs/:graphId/edges', {templateUrl: 'partials/edge-list.html', controller: 'EdgeListCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/edges', {
          templateUrl: 'partials/edge-list.html',
          controller: 'EdgeListCtrl',
          access: access.ROLE_USER,
          reloadOnSearch: false
        }).
        when('/graphs/:graphId/edges/:edgeId', {templateUrl: 'partials/edge-detail.html', controller: 'EdgeDetailCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/create_edge/:vertexId', {templateUrl: 'partials/edge-create.html', controller: 'EdgeCreateCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/create_edge', {templateUrl: 'partials/edge-create.html', controller: 'EdgeCreateCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/edges/:edgeId/edit_edge', {templateUrl: 'partials/edge-edit.html', controller: 'EdgeEditCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/analytics', {templateUrl: 'partials/analytics/index.html', controller: 'AnalyticsListCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/analytics/:analyticsId', {templateUrl: 'partials/analytics/show.html', controller: 'AnalyticsDetailCtrl', access: access.ROLE_USER}).
        when('/projects', {templateUrl: 'partials/project-list.html', controller: 'ProjectListCtrl', access: access.ROLE_USER}).
        when('/projects/create', {templateUrl: 'partials/project-create.html', controller: 'ProjectCreateCtrl', access: access.ROLE_USER}).
        when('/projects/:projectId', {templateUrl: 'partials/project-detail.html', controller: 'ProjectDetailCtrl', access: access.ROLE_USER}).
        when('/projects/:projectId/history', {templateUrl: 'partials/history/show.html', controller: 'HistoryDetailCtrl', access: access.ROLE_USER}).
        otherwise({redirectTo: '/home'});
  }]).
  config([
    '$provide', function($provide) {
      return $provide.decorator('$rootScope', [
        '$delegate', function($delegate) {
          $delegate.safeApply = function(fn) {
            var phase = $delegate.$$phase;
            if (phase === "$apply" || phase === "$digest") {
              if (fn && typeof fn === 'function') {
                fn();
              }
            } else {
              $delegate.$apply(fn);
            }
          };
          return $delegate;
        }
      ]);
    }
  ]).
  config(['$httpProvider', function($httpProvider) {
    var interceptor = ['$rootScope','$q', function(scope, $q) {

      function success(response) {
        return response;
      }

      function error(response) {
        // notify user if login incorrect
        if (response.config.url === scope.url_login){
          alert("Username or Password Incorrect!");
        }

        var status = response.status;
        if (status === 401) {
          var deferred = $q.defer();
          var req = {
            config: response.config,
            deferred: deferred
          }
          scope.requests401.push(req);
          scope.$broadcast('event:loginRequired');
          return deferred.promise;
        }
        else if (status === 403)
        {
          //FIXME : Kathik's lame ass attempt to handle a 403.
          var deferred = $q.defer();
          var req = {
            config: response.config,
            deferred: deferred
          }
          scope.requests401.push(req);
          scope.projectMsgError = "Unauthorized Access";

          //FIXME : Add a custom event that they can customize to do fancier error handling
          scope.$broadcast('event:loginRequired');
          return deferred.promise;
        }
        // otherwise
        return $q.reject(response);

      }

      return function(promise) {
        return promise.then(success, error);
      }

    }];

    // add interceptor to app
    $httpProvider.responseInterceptors.push(interceptor);
    $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';

  }]).
  constant('appConfig', {
    elasticSearch: {
      fieldSize: 10,
      sorting: {
        direction: "asc"
      }
    },
    historyServer: {
        host: "localhost",
        port: 8448,
        storage: "/tmp/dendrite/history",
        defaultFileFormat: 'GraphSON'
    },
    branches: {
      metadata: {
        pollTimeout: 5 * 1000
      }
    },
    algorithms: {
      'edgeDegrees': {
        'category': 'connectedness',
        'destructive': true,
        'description': 'The <a href="http://en.wikipedia.org/wiki/Degree_(graph_theory)">degree</a> of a graph vertex v of a graph G is the number of graph edges which touch v.',
        'form': 'partials/analytics/connectedness/edge_degrees/form.html',
        'defaults': {
          'analyticEngine': 'titan'
        },
        'example': {
          'img': 'partials/analytics/connectedness/edge_degrees/example/UndirectedDegrees_(Loop).svg',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/d/d6/UndirectedDegrees_%28Loop%29.svg'
        }
      },
      'sssp': {
        'category': 'connectedness',
        'destructive': true,
          'description': '<a href="http://en.wikipedia.org/wiki/File:Shortest_path_with_direct_weights.svg">Shortest Path</a> computes the shortest path to a single source from all vertices in the graph',
        'form': 'partials/analytics/connectedness/shortest_path/form.html',
        'defaults': {
          'sourceVertex': 'emptyValue'
        },
        'example': {
          'img': 'partials/analytics/connectedness/shortest_path/example/Shortest_path_with_direct_weights.svg',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/3/3b/Shortest_path_with_direct_weights.svg'
        }
      },
      'pagerank': {
        'category': 'key_players',
        'destructive': true,
        'description': '<a href="http://en.wikipedia.org/wiki/PageRank">PageRank</a> is a link analysis algorithm that assigns a numerical weighting to each element of a hyperlinked set of documents, such as the World Wide Web, with the purpose of "measuring" its relative importance within the set. The algorithm may be applied to any collection of entities with reciprocal quotations and references.\
        <p class="margin-top">The damping factor represents the probability that a user will continue following links connected to the current page instead of navigating to a random URL.  For example, a damping factor of 0.85 indicates a 15% chance for the user to visit a random URL instead of clicking a link on the current page.</p>',
        'form': 'partials/analytics/key_players/pagerank/form.html',
        'defaults': {
          'analyticEngine': 'jung',
          'dampingFactor': 0.85
        },
        'example': {
          'img': 'partials/analytics/key_players/pagerank/example/PageRanks-Example.svg',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/f/fb/PageRanks-Example.svg'
        }
      },
      'betweennessCentrality': {
        'category': 'key_players',
        'destructive': true,
        'description': '<a href="http://en.wikipedia.org/wiki/Centrality#Betweenness_centrality">Betweenness centrality</a> is a measure of a node\'s centrality in a network. It is equal to the number of shortest paths from all vertices to all others that pass through that node. Betweenness centrality is a more useful measure (than just connectivity) of both the load and importance of a node.\
        <p class="margin-top"><strong>Note</strong>: This does not scale with large graphs as the complexity of the algorithm is O(n^2 + nm).</p>',
        'form': 'partials/analytics/key_players/betweenness_centrality/form.html',
        'defaults': {},
        'example': {
          'img': 'partials/analytics/key_players/betweenness_centrality/example/800px-Social_graph.gif',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/thumb/d/de/Social_graph.gif/800px-Social_graph.gif'
        }
      },
      'closenessCentrality': {
        'category': 'key_players',
        'destructive': false,
        'description': '<a href="http://en.wikipedia.org/wiki/Centrality#Closeness_centrality">Closeness centrality</a> is the measure of the inverse of the sum of the distance to all other nodes. Closeness can be regarded as the measure of how long it will take for information to propogate from one node through the network.\
        <p class="margin-top"><strong>Note</strong>: This does not scale with large graphs as the complexity of the algorithm is O(n^2 + nm).</p>',
        'form': 'partials/analytics/key_players/closeness_centrality/form.html',
        'defaults': {},
        'example': {
          'img': 'partials/analytics/key_players/closeness_centrality/example/240px-Graph_betweenness.svg.png',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Graph_betweenness.svg/240px-Graph_betweenness.svg.png'
        }
      },
      'eigenvectorCentrality': {
        'category': 'key_players',
        'destructive': false,
        'description': '<a href="http://en.wikipedia.org/wiki/Centrality#Eigenvector_centrality">Eigenvector centrality</a> is a measure of the influence of a node in a network. It assigns relative scores to all nodes in the network based on the concept that connections to high-scoring nodes contribute more to the score of the node in question than equal connections to low-scoring nodes.',
        'form': 'partials/analytics/key_players/eigenvector_centrality/form.html',
        'defaults': {},
        'example': {
          'img': 'partials/analytics/key_players/eigenvector_centrality/example/Apex_rhombic_dodecahedron.svg',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/7/7e/Apex_rhombic_dodecahedron.svg'
        }
      },
      'barycenterDistance': {
        'category': 'key_players',
        'destructive': true,
        'description': 'The barycenter scorer assigns a score to each vertex that is the sum of distances to all other vertexes.',
        'form': 'partials/analytics/key_players/barycenter_distance/form.html',
        'defaults': {},
        'example': {
          'img': 'partials/analytics/key_players/barycenter_distance/example/5n_PERT_graph_with_critical_path.svg',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/9/91/5n_PERT_graph_with_critical_path.svg'
        }
      },
      'TSC': {
        'category': 'key_players',
        'destructive': true,
        'description': 'The Total Subgraph Communicability is a measurement of how well each node communicates with the other nodes of the network.  It represents the ease at which a node can send information across the network.',
        'form': 'partials/analytics/key_players/total_subgraph_communicability/form.html',
        'defaults': {},
        'example': {
          'img': 'partials/analytics/key_players/total_subgraph_communicability/example/800px-Interest_graph.gif',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/thumb/c/c9/Interest_graph.gif/800px-Interest_graph.gif'
        }
      },
      'snapCentrality': {
        'category': 'key_players',
        'destructive': true,
        'description': 'The <a href="http://snap.stanford.edu">Stanford Network Analysis Platform (SNAP)</a> has a wide variety of Graph Analytic Algorithms, including Centrality.  SNAP\'s centrality will calculate Degree, Closeness, Betweenness, EigenVector, NetworkConstraint, ClusteringCoefficient, PageRank, HubScore, and AuthorityScore.',
        'form': 'partials/analytics/key_players/snap_centrality/form.html',
        'defaults': {},
        'example': {
          'img': 'partials/analytics/key_players/snap_centrality/example/Random-graph-Erdos_generated_network.svg',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/5/5f/Random-graph-Erdos_generated_network.svg'
        }
      },
      'connected_component': {
        'category': 'community',
        'destructive': true,
        'description': 'A <a href="http://en.wikipedia.org/wiki/Connected_component_(graph_theory)">connected component</a> is a group of vertices such that there is a path between each vertex in the component and all other vertices in the group. If two vertices are in different connected components there is no path between them.',
        'form': 'partials/analytics/community/connected_component/form.html',
        'defaults': {},
        'example': {
          'img': 'partials/analytics/community/connected_component/example/Pseudoforest.svg',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/8/85/Pseudoforest.svg'
        }
      },
      'connected_component_stats': {
        'category': 'community',
        'destructive': true,
        'description': 'Calculates a histogram of component sizes.  A connected component is a group of vertices such that there is a path between each vertex in the component and all other vertices in the group. If two vertices are in different connected components there is no path between them.',
        'form': 'partials/analytics/community/connected_component_stats/form.html',
        'defaults': {},
        'example': {
          'img': 'partials/analytics/community/connected_component_stats/example/Histogram-ETTR.png',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/a/aa/Histogram-ETTR.png'
        }
      },
      'simple_coloring': {
        'category': 'community',
        'destructive': false,
        'description': '<a href="http://en.wikipedia.org/wiki/Graph_coloring">Graph coloring</a> is a special case of graph labeling; it is an assignment of labels traditionally called "colors" to elements of a graph subject to certain constraints. In its simplest form, it is a way of coloring the vertices of a graph such that no two adjacent vertices share the same color; this is called a vertex coloring.',
        'form': 'partials/analytics/community/simple_coloring/form.html',
        'defaults': {},
        'example': {
          'img': 'partials/analytics/community/simple_coloring/example/Petersen_graph_3-coloring.svg',
          'ref': 'http://upload.wikimedia.org/wikipedia/commons/9/90/Petersen_graph_3-coloring.svg'
        }
      }
    },

    analytics: {
        metadata: {
          pollTimeout: 500
        },
        BarycenterScore: { },
        BetweennessCentrality: { },
        ClosenessCentrality: { },
        EigenvectorCentrality: { },
        PageRank: {
          dampingFactor: 0.85
        },
        GraphLab: {
          algorithm: 'pagerank'
        },
        Snap: {
          algorithm: 'centrality'
        },
        EdgeDegrees: {
          analyticEngine: 'titan'
        }
    },
    // **disclaimer: UI file parsing an experimental demo feature until server-side
    //    functionality complete.  UI parsing relies on client-side file import, as well as
    //    wildcard regex and brute force array iteration.  UI parsing also uses a custom
    //    word separator (which could confuse JSON responses if keeping formatted values)
    //    to compensate for GML using space-separated key-value pairs
    //
    //    Disable if importing large graph files and/or if concerned about client browser
    //    performance
    fileUpload: {
      parseGraphFile: true,
      parseSeparator: ":::",
      maxBytesLocal: Math.pow(1024,3)
    }
  }).
  run(['$rootScope', '$http', '$location', 'User', function(scope, $http, $location, User) {
    // store requests which failed due to 401 response.
    scope.requests401 = [];

    // store auth URLs
    scope.url_login = 'j_spring_security_check';
    scope.url_logout = 'j_spring_security_logout';

    /*
    // event:loginConfirmed - resend all the 401 requests.
    scope.$on('event:loginConfirmed', function() {
      var i, requests = scope.requests401;
      for (i = 0; i < requests.length; i++) {
        retry(requests[i]);
      }
      scope.requests401 = [];

      function retry(req) {
        $http(req.config).then(function(response) {
          req.deferred.resolve(response);
        });
      }
    });
    */

    // event:returnHome - send user to homepage
    scope.$on('event:returnHome', function() {
      $location.path('/home');
      $location.search({});
    });

    // event:loggedIn - action immediately after login
    scope.$on('event:loggedIn', function() {
      $location.path('/projects');
      $location.search({});
    });


    //event:loginRequest - send credentials to the server.
    scope.$on('event:loginRequest', function(event, username, password) {
      var payload = $.param({j_username: username, j_password: password});
      var config = {
        headers: {'Content-Type':'application/json; charset=UTF-8'}
      }
      $http.post(scope.url_login, payload, config).success(function(data) {
        if (data === 'AUTHENTICATION_SUCCESS') {
          scope.$broadcast('event:loginConfirmed');
        }
      });
    });

    // event:loginRequired - redirect to root URL
    scope.$on('event:loginRequired', function() {
      User.resetRole();
      scope.$broadcast('event:returnHome');
    });

    // event: logoutConfirmed - redirect to root URL
    scope.$on('event:logoutConfirmed', function() {
      scope.$broadcast('event:returnHome');
    });

    // event: logoutRequest - invoke logout on the server and broadcast 'event:loginRequired'.
    scope.$on('event:logoutRequest', function() {
      $http.put(scope.url_logout, {}).success(function() {
        ping();
      });
    });

    scope.$on('$viewContentLoaded', function() {
      scope.projectHasData = false;
    });
    scope.$on('event:projectHasData', function() {
      scope.projectHasData = true;
    });

    scope.preventClose = function(event) {
      event.stopPropagation();
    };

    scope.back = function() {
      window.history.back();
    };

  }]);
