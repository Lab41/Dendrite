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
        otherwise({redirectTo: '/graphs'});
  }]);
