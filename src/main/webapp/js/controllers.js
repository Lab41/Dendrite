'use strict';

/* Controllers */

angular.module('dendrite.controllers', []).
    controller('navCtrl', ['$scope', '$location', 'User', 'Graph', function ($scope, $location, User, Graph) {
      $scope.User = User;
      $scope.query = Graph.query();
      
      // update navbar class to highlight link of current page
      $scope.navClass = function (page) {
          var currentRoute = $location.path().substring(1) || 'home';
          return page === currentRoute ? 'active' : '';
      }; 
      
      // replicate LoginCtrl to embed user/pass in navbar
      $scope.login = function() {
        User.login($scope.username, $scope.password).
          success(function(){
            $scope.User = User;
            $scope.$broadcast('event:returnHome');
          }).
          error(function(response){
            window.alert('Authentication failed!', response);
          });
      }; 
    }]).
    controller('HomeCtrl', function($scope, $location, User, Graph) {
      $scope.User = User; 
      $scope.accessLevels = User.accessLevels;     
    }).
    controller('LoginCtrl', function($scope, $location, User) {
      $scope.login = function() {
        User.login($scope.username, $scope.password).
          success(function(){
            $scope.User = User;
            $scope.$broadcast('event:returnHome');
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
            $scope.$broadcast('event:loginRequired');
            //$location.path('/login');
          }).
          error(function(response){
            window.alert('Logout failed!', response);
            //$location.path('/login');
            $scope.$broadcast('event:loginRequired');
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
    controller('VertexListCtrl', function($scope, $routeParams, $filter, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;

        $scope.gridOptions = {
            data: 'vertices',
            columnDefs: [
                {field: '_id', displayName: 'ID'},
                {field: 'name', displayName: 'Name', enableCellEdit: true},
                {field: 'address', displayName: 'Address', enableCellEdit: true},
            ],
            filterOptions: {
                filterText: "",
                useExternalFilter: true
            },
            enablePaging: true,
            showFooter: true,
            pagingOptions: {
                pageSizes: [50, 100],
                pageSize: 50,
                currentPage: 1
            },
            useExternalSorting: true,
            sortInfo: {
                fields: ['_id'],
                directions: ['asc']
            },
            selectedItems: [],
            selectWithCheckboxOnly: true,
            showSelectionCheckbox: true,
            plugins: [new ngGridFlexibleHeightPlugin()]
        };

        $scope.getData = function() {
          Vertex.query({graphId: $routeParams.graphId}, function(query) {
            var results = query.results;

            // We are going to pretend that the server does the sorting and paging. So first sort the data:
            results = $filter('orderBy')(
              results,
              $scope.gridOptions.sortInfo.fields[0],
              $scope.gridOptions.sortInfo.directions[0] === 'asc'
            );

            // Then truncate the data to fit our "page".
            results = results.slice(
              ($scope.gridOptions.pagingOptions.currentPage - 1) * $scope.gridOptions.pagingOptions.pageSize,
              $scope.gridOptions.pagingOptions.currentPage * $scope.gridOptions.pagingOptions.pageSize
            );

            $scope.gridOptions.totalServerItems = query.totalSize;

            $scope.vertices = results;

            // Finally, make sure that the scope is updated.
            if (!$scope.$$phase) {
              $scope.$apply();
            }
          });
        };

        $scope.getData();

        $scope.$watch('gridOptions.filterOptions', function(newVal, oldVal) {
          if (newVal !== oldVal && newVal.currentPage !== oldVal.currentPage) {
            $scope.getData();
          }
        }, true);
        $scope.$watch('gridOptions.pagingOptions', function(newVal, oldVal) {
          if (newVal !== oldVal) {
            $scope.getData();
          }
        }, true);
        $scope.$watch('gridOptions.sortInfo', function(newVal, oldVal) {
          if (newVal !== oldVal) {
            $scope.getData();
          }
        }, true);

        $scope.isDeleteDisabled = true;
        $scope.$watch('gridOptions.selectedItems', function(newVal, oldVal) {
          if (newVal !== oldVal) {
            $scope.isDeleteDisabled = (newVal.length === 0);
          }
        }, true);

        $scope.deleteSelectedVertices = function() {
            $scope.gridOptions.selectedItems.forEach(function(vertex) {
                Vertex.delete({graphId: $scope.graphId, vertexId: vertex._id}, function() {
                    $scope.getData();
                });
            });
            $scope.isDeleteDisabled = true;
        };
    }).
    controller('VertexDetailCtrl', function($scope, $routeParams, $location, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.vertexId = $routeParams.vertexId;
        $scope.query = Vertex.get({graphId: $scope.graphId, vertexId: $scope.vertexId});

        $scope.delete = function() {
            Vertex.delete({graphId: $scope.graphId, vertexId: $scope.query.results._id}, function() {
                $location.path('graphs/' + $scope.graphId + '/vertices');
            });
        }
    }).
    controller('VertexCreateCtrl', function($scope, $routeParams, $location, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;

        $scope.save = function() {
            Vertex.save({graphId: $scope.graphId}, $scope.vertex, function() {
                $location.path('graphs/' + $scope.graphId + '/vertices');
            });
        };
    }).
    controller('EdgeListCtrl', function($scope, $routeParams, Edge) {
        $scope.graphId = $routeParams.graphId;
        $scope.query = Edge.query({graphId: $routeParams.graphId});

        $scope.deleteEdge = function(edge) {
            Edge.delete({graphId: $scope.graphId, edgeId: edge._id}, function() {
               $scope.vertices.splice($scope.edges.indexOf(edge), 1);
            });
        }
    }).
    controller('EdgeDetailCtrl', function($scope, $routeParams, Edge) {
        $scope.graphId = $routeParams.graphId;
        $scope.edgeId = $routeParams.edgeId;
        $scope.query = Edge.get({graphId: $routeParams.graphId, edgeId: $scope.edgeId});

        $scope.delete = function() {
            Edge.delete({graphId: $scope.graphId, edgeId: $scope.edge._id}, function() {
                $location.path('graphs/' + $scope.graphId + '/edges');
            });
        }
    }).
    controller('EdgeCreateCtrl', function($scope, $routeParams, $location, Edge) {
        $scope.graphId = $routeParams.graphId;

        $scope.save = function() {
            Edge.save({graphId: $scope.graphId}, $scope.edge, function() {
                $location.path('graphs/' + $scope.graphId + '/edges');
            });
        };
    }).
    controller('FileUploadCtrl', function ($scope, $routeParams) {
        $scope.graphId = $routeParams.graphId;
        $scope.fileUploaded = false;

        $scope.uploadFile = function(content, completed) {
            if (completed) {
                $scope.fileUploaded = true;
                if (content.status === "ok") {
                    $scope.uploadMessage = "file uploaded";
                } else {
                    $scope.uploadMessage = "upload failed: " + content.msg;
                }
            }
        }
    });
