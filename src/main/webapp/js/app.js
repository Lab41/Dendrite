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
        when('/login', {templateUrl: 'partials/login.html', controller: 'LoginCtrl'}).
        when('/graphs', {templateUrl: 'partials/graph-list.html', controller: 'GraphListCtrl'}).
        when('/graphs/:graphId', {templateUrl: 'partials/graph-detail.html', controller: 'GraphDetailCtrl'}).
        when('/graphs/:graphId/vertices', {templateUrl: 'partials/vertex-list.html', controller: 'VertexListCtrl'}).
        when('/graphs/:graphId/vertices/:vertexId', {templateUrl: 'partials/vertex-detail.html', controller: 'VertexDetailCtrl'}).
        when('/graphs/:graphId/create_vertex', {templateUrl: 'partials/vertex-create.html', controller: 'VertexCreateCtrl'}).
        otherwise({redirectTo: '/graphs'});
  }]).
  // redirect all unauthenticated users to login page, regardless of intended route
  run(function($rootScope, $location, User) {
    return $rootScope.$on('$routeChangeStart', function(event, next, current) {
      if (!User.isAuthenticated() && next.templateUrl !== '/partials/login.html') {
        return $location.path("/login");
      }
    });
  });
