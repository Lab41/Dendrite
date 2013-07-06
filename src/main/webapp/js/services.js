'use strict';

/* Services */


// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('dendrite.services', ['ngResource']).
    factory('Graph', function($resource){
        return $resource('rexster-resource/graphs/:graphId', {}, {
            query: {method: 'GET', params: {graphId: '@graphId'}, isArray: false}
        });
    }).
    factory('Vertex', function($resource){
        $resource('rexster-resource/graphs/titanexample/vertices/:vertexId', {}, {
            query: {method: 'GET', params: {vertexId: 'vertices'}, isArray: true}
        });
    });
