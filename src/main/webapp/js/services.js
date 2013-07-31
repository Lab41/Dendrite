'use strict';

/* Services */

angular.module('dendrite.services', ['ngResource']).
    // User factory to handle authentication
    factory('User', function($http, $location) {
      var _this = this;
      this.authenticated = false;
      this.name = null;
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
          return $http.post('j_spring_security_check', {
            j_username: username,
            j_password: password
          }).success(function(response){
            if (response.authenticated) {
              _this.name = username;
              _this.authenticated = true;
            }
          });
        },

        // logout posts to spring logout URL
        // callback remained undefined; apply location.path directly
        logout: function() {
          return $http.post('j_spring_security_logout', {}).
            success(function(response) {
              _this.authenticated = false;
            });
        }
      };
    }).
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
