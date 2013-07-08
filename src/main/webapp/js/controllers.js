'use strict';

/* Controllers */

angular.module('dendrite.controllers', []).
    controller('GraphListCtrl', function($scope, Graph) {
        $scope.graphs = Graph.query();
    }).
    controller('GraphDetailCtrl', function($scope, $routeParams, Graph) {
        $scope.graphId = $routeParams.graphId;
        $scope.graph = Graph.get({graphId: $routeParams.graphId});
    }).
    controller('VertexListCtrl', function($scope, $routeParams, Vertex) {
        $scope.graphId = $routeParams.graphId;
        $scope.vertices = Vertex.query({graphId: $routeParams.graphId});
    }).
    controller('VertexDetailCtrl', function($scope, $routeParams, Vertex) {
        $scope.graphId = $routeParams.graphId;
        $scope.vertex = Vertex.get({graphId: $routeParams.graphId, vertexId: $routeParams.vertexId});
    }).
    controller('VertexCreateCtrl', function($scope, $routeParams, $location, Vertex) {
        $scope.graphId = $routeParams.graphId;
        $scope.vertex = new Vertex();
        $scope.create = function() {
            $scope.vertex.$save({graphId: $scope.graphId}, function() {
                var path = 'graphs/' + $scope.graphId + '/vertices';
                $location.path(path);
            });
        };
    });
