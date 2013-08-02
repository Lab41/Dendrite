'use strict';

/* Controllers */

angular.module('dendrite.controllers', []).
    controller('LoginCtrl', function($scope, $location, User) {
      $scope.login = function() {
        User.login($scope.username, $scope.password).
          success(function(){
            $location.path('/graphs');
          }).
          error(function(response){
            window.alert('Authentication failed!', response);
          });
      };
    }).
    controller('LogoutCtrl', function($scope, $location, User) {
      $scope.logout = function() {
        User.logout().
          success(function(){
            $location.path('/login');
          }).
          error(function(response){
            window.alert('Logout failed!', response);
            $location.path('/login');
          });
      };
    }).
    controller('GraphListCtrl', function($scope, User, Graph) {
        $scope.User = User;
        $scope.query = Graph.query();
    }).
    controller('GraphDetailCtrl', function($scope, $routeParams, User, Graph) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.graph = Graph.get({graphId: $routeParams.graphId});
    }).
    controller('VertexListCtrl', function($scope, $routeParams, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.query = Vertex.query({graphId: $routeParams.graphId});

        $scope.deleteVertex = function(vertex) {
            Vertex.delete({graphId: $scope.graphId, vertexId: vertex._id}, function() {
               $scope.query.results.splice($scope.query.results.indexOf(vertex), 1);
            });
        }
    }).
    controller('VertexDetailCtrl', function($scope, $routeParams, $location, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.vertexId = $routeParams.vertexId;
        $scope.query = Vertex.get({graphId: $scope.graphId, vertexId: $scope.vertexId});

        $scope.delete = function() {
            Vertex.delete({graphId: $scope.graphId, vertexId: $scope.query.results._id}, function() {
                $location.path('graphs/' + $scope.graphId + '/vertices');
            });
        }
    }).
    controller('VertexCreateCtrl', function($scope, $routeParams, $location, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;

        $scope.save = function() {
            Vertex.save({graphId: $scope.graphId}, $scope.vertex, function() {
                $location.path('graphs/' + $scope.graphId + '/vertices');
            });
        };
    }).
    controller('EdgeListCtrl', function($scope, $routeParams, Edge) {
        $scope.graphId = $routeParams.graphId;
        $scope.query = Edge.query({graphId: $routeParams.graphId});

        $scope.deleteEdge = function(edge) {
            Edge.delete({graphId: $scope.graphId, edgeId: edge._id}, function() {
               $scope.vertices.splice($scope.edges.indexOf(edge), 1);
            });
        }
    }).
    controller('EdgeDetailCtrl', function($scope, $routeParams, Edge) {
        $scope.graphId = $routeParams.graphId;
        $scope.edgeId = $routeParams.edgeId;
        $scope.query = Edge.get({graphId: $routeParams.graphId, edgeId: $scope.edgeId});

        $scope.delete = function() {
            Edge.delete({graphId: $scope.graphId, edgeId: $scope.edge._id}, function() {
                $location.path('graphs/' + $scope.graphId + '/edges');
            });
        }
    }).
    controller('EdgeCreateCtrl', function($scope, $routeParams, $location, Edge) {
        $scope.graphId = $routeParams.graphId;

        $scope.save = function() {
            Edge.save({graphId: $scope.graphId}, $scope.edge, function() {
                $location.path('graphs/' + $scope.graphId + '/edges');
            });
        };
    });
