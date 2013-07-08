'use strict';

/* Services */

angular.module('dendrite.services', ['ngResource']).
    factory('Graph', function($resource) {
        return $resource('rexster-resource/graphs/:graphId', {}, {
            query: {method: 'GET', params: {graphId: '@graphId'}, isArray: false}
        });
    }).
    factory('Vertex', function($resource) {
        return $resource('rexster-resource/graphs/:graphId/vertices/:vertexId', {}, {
            query: {method: 'GET', params: {graphId: '@graphId', vertexId: '@vertexId'}, isArray: false}
        });
    });
