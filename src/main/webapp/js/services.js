'use strict';

/* Services */

angular.module('dendrite.services', ['ngResource']).
    // User factory to handle authentication
    factory('User', function($rootScope, $http, $location, $cookieStore) {
      var _this = this;
      this.authenticated = false;
      this.name = null;
      
      var accessLevels = routingConfig.accessLevels
          , userRoles = routingConfig.userRoles
          , currentUser = $cookieStore.get('user') || { username: '', role: userRoles.ROLE_PUBLIC };

      $cookieStore.remove('user');
      
      function changeUser(user) {
          _.extend(currentUser, user);
      };
    
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
              // set User properties, including role from server JSON response
              _this.name = username;
              _this.authenticated = true;
              currentUser.role = userRoles[response.authorities[0].authority];

              // alert app to successful login
              $rootScope.$broadcast('event:loginConfirmed');
            }
          });
        },

        // logout posts to spring logout URL
        // on success, broadcast to app
        logout: function() {
          return $http.post($rootScope.url_logout, {}).
            success(function(response) {
              _this.authenticated = false;
              currentUser.role = userRoles.ROLE_PUBLIC;
              $cookieStore.remove('user');
              $rootScope.$broadcast('event:logoutConfirmed');
            });
        },
        
        //TODO: implement SpringMVC "ping" route to return ROLE_xxx
        getRole: function() {
          /*
          $http.get('user/roles').success(function(response) {
            currentUser.role = userRoles[response.authorities[0].authority];
          });
          */
          return currentUser.role;          
        },

        // resetRole reverts current user's role to public access
        resetRole: function() {
          return currentUser.role = userRoles.ROLE_ANON;
        },
                   
        authorize: function(accessLevel, role) {
            if(role === undefined)
                role = currentUser.role;
            if(accessLevel === undefined)
                accessLevel = userRoles.ROLE_PUBLIC;
            return accessLevel.bitMask & role.bitMask;
        },
        
        isLoggedIn: function(user) {
            if(user === undefined)
                user = currentUser;
            return user.role == userRoles.ROLE_USER || user.role == userRoles.ROLE_ADMIN;
        },
        
        accessLevels: accessLevels,
        userRoles: userRoles,
        user: currentUser
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
 
 
 
 
 
