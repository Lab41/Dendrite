'use strict';


// Declare app level module which depends on filters, and services
angular.module('dendrite', [
        'dendrite.filters',
        'dendrite.services',
        'dendrite.directives',
        'dendrite.controllers'
    ]).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.
        when('/graphs', {templateUrl: 'partials/graph-list.html', controller: 'GraphListCtrl'}).
        when('/graphs/:graphId', {templateUrl: 'partials/graph-detail.html', controller: 'GraphDetailCtrl'}).
        when('/graphs/:graphId/vertices', {templateUrl: 'partials/vertex-list.html', controller: 'VertexListCtrl'}).
        when('/graphs/:graphId/vertices/:vertexId', {templateUrl: 'partials/vertex-detail.html', controller: 'VertexDetailCtrl'}).
        when('/graphs/:graphId/create_vertex', {templateUrl: 'partials/vertex-create.html', controller: 'VertexCreateCtrl'}).
        otherwise({redirectTo: '/graphs'});
  }]);
