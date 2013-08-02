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
        when('/graphs/:graphId/edges', {templateUrl: 'partials/edge-list.html', controller: 'EdgeListCtrl'}).
        when('/graphs/:graphId/edges/:edgeId', {templateUrl: 'partials/edge-detail.html', controller: 'EdgeDetailCtrl'}).
        when('/graphs/:graphId/create_edge', {templateUrl: 'partials/edge-create.html', controller: 'EdgeCreateCtrl'}).
        otherwise({redirectTo: '/graphs'});
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
      $location.path("/login");
    });
    
    // event: logoutConfirmed - redirect to root URL
    scope.$on('event:logoutConfirmed', function() {
      $location.path("/login");
      alert("Thanks for Dendrite-ing!  Come back soon, y'all!");
    });
       
    // event: logoutRequest - invoke logout on the server and broadcast 'event:loginRequired'.
    scope.$on('event:logoutRequest', function() {
      $http.put(scope.url_logout, {}).success(function() {
        ping();
      });
    });
   
    // placeholder function to test login status
    function ping() {
      $http.get('rest/ping').success(function() {
        scope.$broadcast('event:loginConfirmed');
      });
    }
    //ping();
    
  }]);
