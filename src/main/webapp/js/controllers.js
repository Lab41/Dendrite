'use strict';

/* Controllers */

angular.module('dendrite.controllers', []).
    controller('LoginCtrl', function($scope, $location, User) {
      $scope.login = function() {
        User.login($scope.username, $scope.password).
          success(function(){
            $location.path('/graphs');
          }).
          error(function(response){
            window.alert('Authentication failed!', response);
          });
      };
    }).
    controller('LogoutCtrl', function($scope, $location, User) {
      $scope.logout = function() {
        User.logout().
          success(function(){
            $location.path('/login');
          }).
          error(function(response){
            window.alert('Logout failed!', response);
            $location.path('/login');
          });
      };
    }).
    controller('GraphListCtrl', function($scope, User, Graph) {
        $scope.User = User;
        $scope.query = Graph.query();
    }).
    controller('GraphDetailCtrl', function($scope, $routeParams, User, Graph) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.graph = Graph.get({graphId: $routeParams.graphId});
    }).
    controller('VertexListCtrl', function($scope, $routeParams, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.vertices = Vertex.query({graphId: $routeParams.graphId});
    }).
    controller('VertexDetailCtrl', function($scope, $routeParams, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.vertex = Vertex.get({graphId: $routeParams.graphId, vertexId: $routeParams.vertexId});
    }).
    controller('VertexCreateCtrl', function($scope, $routeParams, $location, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.vertex = new Vertex();
        $scope.create = function() {
            $scope.vertex.$save({graphId: $scope.graphId}, function() {
                var path = 'graphs/' + $scope.graphId + '/vertices';
                $location.path(path);
            });
        };
    });
