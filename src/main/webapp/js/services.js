'use strict';

/* Services */

angular.module('dendrite.services', ['ngResource']).
    // User factory to handle authentication
    factory('User', function($rootScope, $http, $location) {
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
          return $http.post($rootScope.url_login, {
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
        // on success, broadcast to app
        logout: function() {
          return $http.post($rootScope.url_logout, {}).
            success(function(response) {
              _this.authenticated = false;
              $rootScope.$broadcast('event:logoutConfirmed');
            });
        }
      };
    }).
    factory('Graph', function($resource) {
        return $resource('rexster-resource/graphs/:graphId', {
            graphId: '@name'
        }, {
            query: {
                method: 'GET',
                isArray: false
            }
        });
    }).
    factory('Vertex', function($resource) {
        return $resource('rexster-resource/graphs/:graphId/vertices/:vertexId', {
            graphId: '@graphId',
            vertexId: '@_id'
        }, {
            save: {
                method: 'POST'
            },
            query: {
                method: 'GET',
                isArray: false
            }
        });
    }).
    factory('Edge', function($resource) {
        return $resource('rexster-resource/graphs/:graphId/edges/:edgeId', {
            graphId: '@graphId',
            edgeId: '@_id'
        }, {
            save: {
                method: 'POST'
            },
            query: {
                method: 'GET',
                isArray: false
            }
        });
    });
