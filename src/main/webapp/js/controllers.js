'use strict';

/* Controllers */

angular.module('dendrite.controllers', []).
    controller('GraphListCtrl', function($scope, Graph) {
        $scope.graphs = Graph.query();
    }).
    controller('GraphDetailCtrl', function($scope, $routeParams, Graph) {
        $scope.graph = Graph.get({graphId: $routeParams.graphId});
    });
