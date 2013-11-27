/**
 * Copyright 2013 In-Q-Tel/Lab41
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      }

      function ping() {
        // Check if we're already authenticated...
        $http.get('api/user').success(function(response) {
          if (response.name === "anonymous") {
            $rootScope.$emit("event:loginRequired");
          } else {
            _this.authenticated = true;
            _this.name = response.name;
            currentUser.role = userRoles[response.authorities[0].authority];
          }
        });
      }

      ping();

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
          var payload = $.param({
            j_username: username,
            j_password: password,
            _spring_security_remember_me: "on"
          });
          var config = {
            headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'}
          };

          return $http.post($rootScope.url_login, payload, config).success(function(response) {
            ping();
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
    factory('Helpers', function($resource) {
        //TODO: delete once backend APIs complete - dummy result array
        var analyticResults = [];

        return {
          //TODO: delete once backend APIs complete - function to add class for progress bars
          colorProgressBars: function(progress) {
            if (progress < 33) {
              return 'danger';
            } else if (progress < 66) {
              return 'warning';
            } else if (progress < 100) {
              return 'info';
            } else {
              return 'success';
            }
          }
        };
    }).
    factory('Analytics', function($resource) {
        //TODO: delete once backend APIs complete - dummy result array
        var analyticResults = [];

        return {
          // TODO: delete once backend APIs complete - reset dummy results
          createDummyResults: function() {
            analyticResults = [
              {'id': 0, 'analyticType': 'PageRank', 'percentComplete': 0},
              {'id': 1, 'analyticType': 'Degree Centrality', 'percentComplete': 0},
              {'id': 2, 'analyticType': 'Betweenness Centrality', 'percentComplete': 0},
              {'id': 3, 'analyticType': 'Proximity Prestige', 'percentComplete': 0},
            ];
          },

          // configuration options for various analytics
          analyticConfig: {
            'metadata': {
              'pollTimeout': 750,
              'analyticsExecuting': false
            },

            'PageRank': {
              saved: false,
              dampingFactor: 0.85
            }
          },

          // TODO: delete once backend APIs complete - retrieve dummy result
          getAnalytic: function(id) {
            return analyticResults[id];
          },

          // fire off calculation
          calculate: function(analyticType, attr) {
            alert("calculating " + analyticType + " with input " + attr.dampingFactor);
            this.analyticConfig.metadata.analyticsExecuting = true;
            this.createDummyResults();
            /*
            //TODO enable once backend APIs complete
            return $resource('rexster-resource/graphs/:graphId/analytics/execute', {
                graphId: '@name',
                analyticParams: attr
            }, {
                query: {
                    method: 'GET',
                    isArray: false
                }
            });
            */
          },

          // poll server for active calculations
          pollActive: function() {
            console.log("polling for active analytics");

            if (this.analyticConfig.metadata.analyticsExecuting) {
              this.analyticConfig.metadata.analyticsExecuting = false;

              //TODO: delete once backend APIs complete - dummy function to simulate job progress
              for (var i = 0; i < analyticResults.length; i++) {
                if (analyticResults[i].percentComplete < 100) {
                  this.analyticConfig.metadata.analyticsExecuting = true;
                  var newNum = analyticResults[i].percentComplete += Math.floor((Math.random()*15)+1);
                  analyticResults[i].percentComplete = (newNum > 100) ? 100 : newNum;
                }
              }
            }
            return analyticResults;
            /*
            //TODO enable once backend APIs complete
            return $resource('rexster-resource/graphs/:graphId/analytics/poll', {
                graphId: '@name',
            }, {
                query: {
                    method: 'GET',
                    isArray: false
                }
            });
            */

          },
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
                url: 'rexster-resource/graphs/:graphId/vertices',
                method: 'POST'
            },
            show: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId',
                method: 'GET',
                isArray: false
            },
            update: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId',
                method: 'PUT'
            },
            delete: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId',
                method: 'DELETE'
            },
            query: {
                method: 'GET',
                isArray: false
            },
            queryConnectedVertices: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/both',
                method: 'GET',
                isArray: false
            },
            queryConnectedInVertices: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/in',
                method: 'GET',
                isArray: false
            },
            queryConnectedOutVertices: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/out',
                method: 'GET',
                isArray: false
            },
            queryEdges: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/bothE',
                method: 'GET',
                isArray: false
            },
            queryInEdges: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/inE',
                method: 'GET',
                isArray: false
            },
            queryOutEdges: {
                url: 'rexster-resource/graphs/:graphId/vertices/:vertexId/outE',
                method: 'GET',
                isArray: false
            },
            list: {
                url: 'rexster-resource/graphs/:graphId/vertices',
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
            update: {
                url: 'rexster-resource/graphs/:graphId/edges/:edgeId',
                method: 'PUT'
            },
            delete: {
                url: 'rexster-resource/graphs/:graphId/edges/:edgeId',
                method: 'DELETE'
            },
            query: {
                method: 'GET',
                isArray: false
            }
        });
    });
