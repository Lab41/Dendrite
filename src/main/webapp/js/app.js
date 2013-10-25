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

// Declare app level module which depends on filters, and services
angular.module('dendrite', [
        'dendrite.filters',
        'dendrite.services',
        'dendrite.directives',
        'dendrite.controllers',
        'ngCookies',
        'ngUpload',
        'ngGrid',
        '$strap.directives'
    ]).
  config(['$routeProvider', function($routeProvider) {
    var access = routingConfig.accessLevels;
    $routeProvider.
        when('/home', {templateUrl: 'partials/home.html', controller: 'HomeCtrl', access: access.ROLE_ANON}).
        when('/login', {templateUrl: 'partials/login.html', controller: 'LoginCtrl', access: access.ROLE_ANON}).
        when('/graphs', {templateUrl: 'partials/graph-list.html', controller: 'GraphListCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId', {templateUrl: 'partials/graph-detail.html', controller: 'GraphDetailCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/vertices', {
          templateUrl: 'partials/vertex-list.html',
          controller: 'VertexListCtrl',
          access: access.ROLE_USER,
          reloadOnSearch: false
        }).
        when('/graphs/:graphId/vertices/:vertexId', {templateUrl: 'partials/vertex-detail.html', controller: 'VertexDetailCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/create_vertex', {templateUrl: 'partials/vertex-create.html', controller: 'VertexCreateCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/edges', {templateUrl: 'partials/edge-list.html', controller: 'VertexListCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/edges/:edgeId', {templateUrl: 'partials/edge-detail.html', controller: 'EdgeDetailCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/create_edge/:vertexId', {templateUrl: 'partials/edge-create.html', controller: 'EdgeCreateCtrl', access: access.ROLE_USER}).
        when('/graphs/:graphId/create_edge', {templateUrl: 'partials/edge-create.html', controller: 'EdgeCreateCtrl', access: access.ROLE_USER}).
        otherwise({redirectTo: '/home'});
  }]).
  config(['$httpProvider', function($httpProvider) {
    var interceptor = ['$rootScope','$q', function(scope, $q) {

      function success(response) {
        return response;
      }

      function error(response) {
        // notify user if login incorrect
        if (response.config.url === scope.url_login){
          alert("Username or Password Incorrect!");
        }

        var status = response.status;
        if (status === 401) {
          var deferred = $q.defer();
          var req = {
            config: response.config,
            deferred: deferred
          }
          scope.requests401.push(req);
          scope.$broadcast('event:loginRequired');
          return deferred.promise;
        }
        // otherwise
        return $q.reject(response);

      }

      return function(promise) {
        return promise.then(success, error);
      }

    }];

    // add interceptor to app
    $httpProvider.responseInterceptors.push(interceptor);
  }]).
  run(['$rootScope', '$http', '$location', 'User', function(scope, $http, $location, User) {
    // store requests which failed due to 401 response.
    scope.requests401 = [];

    // store auth URLs
    scope.url_login = 'j_spring_security_check';
    scope.url_logout = 'j_spring_security_logout';

    /*
    // event:loginConfirmed - resend all the 401 requests.
    scope.$on('event:loginConfirmed', function() {
      var i, requests = scope.requests401;
      for (i = 0; i < requests.length; i++) {
        retry(requests[i]);
      }
      scope.requests401 = [];

      function retry(req) {
        $http(req.config).then(function(response) {
          req.deferred.resolve(response);
        });
      }
    });
    */

    // event:returnHome - send user to homepage
    scope.$on('event:returnHome', function() {
      $location.path('/home');
      $location.search({});
    });


    //event:loginRequest - send credentials to the server.
    scope.$on('event:loginRequest', function(event, username, password) {
      var payload = $.param({j_username: username, j_password: password});
      var config = {
        headers: {'Content-Type':'application/json; charset=UTF-8'}
      }
      $http.post(scope.url_login, payload, config).success(function(data) {
        if (data === 'AUTHENTICATION_SUCCESS') {
          scope.$broadcast('event:loginConfirmed');
        }
      });
    });

    // event:loginRequired - redirect to root URL
    scope.$on('event:loginRequired', function() {
      User.resetRole();
      scope.$broadcast('event:returnHome');
    });

    // event: logoutConfirmed - redirect to root URL
    scope.$on('event:logoutConfirmed', function() {
      scope.$broadcast('event:returnHome');
    });

    // event: logoutRequest - invoke logout on the server and broadcast 'event:loginRequired'.
    scope.$on('event:logoutRequest', function() {
      $http.put(scope.url_logout, {}).success(function() {
        ping();
      });
    });

    scope.back = function() {
      window.history.back();
    };

  }]);
