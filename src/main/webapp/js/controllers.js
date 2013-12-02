
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
    controller('GraphDetailCtrl', function($scope, $routeParams, $q, User, Graph, Vertex, Edge) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.graph = Graph.get({graphId: $routeParams.graphId});

        $scope.forceDirectedGraphData = {};

        // This can be removed after we upgrade to angular 1.2.
        //$scope.forceDirectedGraphData = {
        //  vertices: Vertex.query({graphId: $scope.graphId}),
        //  edges: Edge.query({graphId: $scope.graphId})
        //};

        $scope.reloadGraph = function() {
          $scope.forceDirectedGraphData = {
            vertices: $q.defer(),
            edges: $q.defer(),
          };

          Vertex.query({graphId: $scope.graphId}, function(vertices) {
            $scope.forceDirectedGraphData.vertices.resolve(vertices);
          }, function() {
            $scope.forceDirectedGraphData.vertices.reject();
          });

          Edge.query({graphId: $scope.graphId}, function(edges) {
            $scope.forceDirectedGraphData.edges.resolve(edges);
          }, function() {
            $scope.forceDirectedGraphData.edges.reject();
          });
        }

        $scope.reloadGraph();

    }).
    controller('AnalyticsDetailCtrl', function($scope, $location, $routeParams, $filter, $q, User, Vertex, Edge, Analytics, Helpers, $timeout) {
        // config
        $scope.activeAnalytics = [];
        $scope.colorProgressBars = Helpers.colorProgressBars;

        Analytics.createDummyResults();

        // periodically poll for active calculations
        var pollActive = function() {
          console.log(Analytics.analyticConfig.metadata.pollTimeout);
          $scope.activeAnalytics = Analytics.pollActive();
          $scope.analytic = Analytics.getAnalytic($routeParams.analyticsId);
          $timeout(pollActive, Analytics.analyticConfig.metadata.pollTimeout);
        }
        pollActive();
    }).
    controller('AnalyticsFormCtrl', function($scope, $location, $routeParams, $filter, $q, User, Vertex, Edge, Analytics, Helpers, $timeout) {
        // placeholder default attributes

        $scope.$watch('analyticType', function () {
          $scope.attr = Analytics.analyticConfig[$scope.analyticType];
        });

        // submit calculation
        $scope.calculate = function() {
          Analytics.calculate($scope.analyticType, $scope.attr);
        };
    }).
    controller('AnalyticsListCtrl', function($scope, $location, $routeParams, $filter, $q, User, Vertex, Edge, Analytics, Helpers, $timeout) {
        // config
        $scope.activeAnalytics = [];
        $scope.colorProgressBars = Helpers.colorProgressBars;

        // show result
        $scope.showAnalytic = function(id) {
          $location.path('graphs/' + $routeParams.graphId + '/analytics/' + id);
        };

        // periodically poll for active calculations
        var pollActive = function() {
          $scope.activeAnalytics = Analytics.pollActive();
          $timeout(pollActive, Analytics.analyticConfig.metadata.pollTimeout);
        }
        pollActive();
    }).
    controller('VertexListCtrl', function($scope, $location, $routeParams, $filter, $q, User, Vertex, Edge) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.data = [];
        $scope.selectedItems = [];
        $scope.totalServerItems = 0;

        if ($routeParams.mode === undefined || $routeParams.mode === "vertex") {
          var Item = Vertex;
          var columnDefs = [
            //{field: '_id', displayName: 'ID', enableCellEdit: false},
            {field: 'name', displayName: 'Name', enableCellEdit: true},
            //{field: 'type', displayName: 'Type', enableCellEdit: true},
            {field: 'age', displayName: 'Age', enableCellEdit: true},
            //{field: 'lang', displayName: 'Favorite Language', enableCellEdit: true}
          ];
        } else if ($routeParams.mode === "edge") {
          var Item = Edge;
          var columnDefs = [
            {field: '_id', displayName: 'ID', enableCellEdit: false},
            {field: '_label', displayName: 'Label', enableCellEdit: true},
            {field: '_inV', displayName: 'In Vertex', enableCellEdit: true},
            {field: '_outV', displayName: 'Out Vertex', enableCellEdit: true}
          ]
        }

        $scope.gridOptions = {
            data: 'data',
            columnDefs: columnDefs,
            showFilter: false,
            filterOptions: {
                filterText: $routeParams.filterText || '',
                useExternalFilter: true
            },
            enablePaging: true,
            showFooter: true,
            pagingOptions: {
                pageSizes: [10, 25, 50, 100],
                pageSize: 10,
                currentPage: 1
            },
            totalServerItems: 'totalServerItems',
            useExternalSorting: true,
            sortInfo: {
                fields: [$routeParams.sortField || '_id'],
                directions: [$routeParams.sortDirection || 'asc']
            },
            selectedItems: $scope.selectedItems,
            multiSelect: false,
            //selectWithCheckboxOnly: true,
            //showSelectionCheckbox: true,
            //plugins: [new ngGridFlexibleHeightPlugin()]
        };

        // Trigger a refresh when the filter changes.
        $scope.$watch('gridOptions.filterOptions.filterText', function(newVal, oldVal) {
          if (newVal !== oldVal) {
            $location.search('filterText', newVal);
            $scope.reloadData();
          }
        }, true);

        // Trigger a refresh when the page changes.
        $scope.$watch('gridOptions.pagingOptions', function(newVal, oldVal) {
          if (newVal !== oldVal && newVal.currentPage !== oldVal.currentPage) {
            $location.search('currentPage', newVal.currentPage);
            $scope.reloadData();
          }
        }, true);

        // Trigger a refresh when the sort order changes.
        $scope.$watch('gridOptions.sortInfo', function(newVal, oldVal) {
          if (newVal !== oldVal) {
            $location.search('sortField', newVal.fields[0]);
            $location.search('sortDirection', newVal.directions[0]);
            $scope.reloadData();
          }
        }, true);

        // Update the hasSelectedItems when selectedItem changes.
        $scope.$watch('selectedItems', function(newVal, oldVal) {
          if (newVal !== oldVal) {
            $scope.hasSelectedItems = ($scope.selectedItems.length !== 0);
            $scope.selectedVertex = $scope.selectedItems[0];
          }
        }, true);

        // Delete the selected items.
        $scope.deleteSelectedItems = function() {
          $q.all(
            $scope.selectedItems.map(function(vertex) {
              var deferred = $q.defer();
              Vertex.delete({graphId: $scope.graphId, vertexId: vertex._id}, function() {
                deferred.resolve();
              }, function() {
                deferred.reject();
              });
              return deferred.promise;
            })
          ).then(function() {
            // Refresh the data once all the items are deleted.
            $scope.reloadData();
          });

          // Make sure to clear the selected list of items.
          $scope.selectedItems.length = 0;
        };

        var currentId;

        function queryNormal() {
          Vertex.query({graphId: $routeParams.graphId}, function(query) {
              // We are going to pretend that the server does the filtering, sorting, and paging.
              reload(query.results, query.totalSize);
          });
        }

        var queryStyle = "vertices";

        $scope.followEdges = function(element) {
          $routeParams.mode = "edge";
          queryStyle = "edges";
          $scope.reloadData();
          return false; // prevent normal link behavior
        };

        $scope.followInEdges = function() {
          queryStyle = "in-edges";
          $scope.reloadData();
          return false; // prevent normal link behavior
        };

        $scope.followOutEdges = function() {
          queryStyle = "out-edges";
          $scope.reloadData();
          return false; // prevent normal link behavior
        };

        $scope.addVertexEdge = function() {
          $location.path('graphs/' + $scope.graphId + '/create_edge/' + $scope.selectedItems[0]._id);
        };

        $scope.editVertex = function() {
          $location.path('graphs/' + $scope.graphId + '/vertices/' + $scope.selectedItems[0]._id + '/edit_vertex');
        };

        $scope.resetItems = function() {
          $scope.selectedItems.shift();
          queryStyle = "vertices";
          $scope.reloadData();
        }

        $scope.reloadData = function() {

          if (queryStyle === "vertices") {
            Vertex.query({graphId: $routeParams.graphId}, function(query) {
              // We are going to pretend that the server does the filtering, sorting, and paging.
              reload(query);
            });
          } else {
            $q.all(
              $scope.selectedItems.map(function(vertex) {
                var deferred = $q.defer();
                var f;
                if (queryStyle === "edges") {
                  f =  Vertex.queryConnectedVertices;
                } else if (queryStyle === "in-edges") {
                  f =  Vertex.queryConnectedInVertices;
                } else if (queryStyle === "out-edges") {
                  f =  Vertex.queryConnectedOutVertices;
                }
                f({graphId: $scope.graphId, vertexId: vertex._id}, function(vertices) {
                  deferred.resolve(vertices);
                });
                return deferred.promise;
              })
            ).then(function(queries) {
                var query = queries[0];

                // Filter out duplicates...
                var vertexExists = {};
                var results = [];
                query.results.forEach(function(vertex) {
                  if (vertexExists[vertex._id] === undefined) {
                    vertexExists[vertex._id] = vertex._id;
                    results.push(vertex);
                  }

                });
                query.results = results;

                reload(query);
            });
          }
        }

        function reload(query) {
          // So first filter the data:
          var results = $filter('filter')(query.results, $scope.gridOptions.filterOptions.filterText);

          // So first sort the data:
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

          $scope.totalServerItems = query.totalSize;

          $scope.data = results;

          // Finally, make sure that the scope is updated.
          if (!$scope.$$phase) {
            $scope.$apply();
          }
        }

        // Finally, fetch the initial list of data.
        $scope.reloadData();
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
    controller('VertexEditCtrl', function($scope, $routeParams, $location, User, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.vertexId = $routeParams.vertexId;
        $scope.query =
          Vertex.get({graphId: $scope.graphId, vertexId: $scope.vertexId})
                .$then(function(res) {
                  $scope.vertex = res.resource.results;
                });

        $scope.save = function() {
            Vertex.update({graphId: $scope.graphId, vertexId: $scope.vertexId}, $scope.vertex, function() {
                $location.path('graphs/' + $scope.graphId + '/vertices');
            });
        };
    }).
    controller('EdgeListCtrl', function($scope, $location, $routeParams, $filter, User, Edge, Vertex) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;

        $scope.edgeDetail = function(id) {
          $location.path('graphs/' + $scope.graphId + '/edges/' + id);
        };

        //TODO refactor for more efficient on-demand Vertex retrieval
        $scope.getVertex = function(id) {
          if ($scope.vertices.results!==undefined) {
            for (var i=0; i<$scope.vertices.results.length; i++) {
              if ($scope.vertices.results[i]._id===id) {
                return $scope.vertices.results[i].name;
              }
            }
          }
          //var name = Vertex.get({graphId: $routeParams.graphId, vertexId: id});
          return 'name';
        };

        $scope.gridOptions = {
            data: 'edges',
            columnDefs: [
                {field: '_id', displayName: 'ID', enableCellEdit: false, cellTemplate: '<div ng-click="edgeDetail(\'{{row.entity[col.field]}}\')"><a>{{row.entity[col.field]}}<a></div>'},
                {field: '_inV', displayName: 'From', enableCellEdit: true, cellTemplate: '<div>{{getVertex( row.entity[col.field] )}}</div>'},
                {field: '_label', displayName: 'Link', enableCellEdit: true},
                {field: '_outV', displayName: 'To', enableCellEdit: true, cellTemplate: '<div>{{getVertex( row.entity[col.field] )}}</div>'},

                //{field: 'weight', displayName: 'Edge Weight', enableCellEdit: true},
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


        $scope.reloadData = function() {
          Edge.query({graphId: $routeParams.graphId}, function(query) {
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

            $scope.edges = results;


            // Finally, make sure that the scope is updated.
            if (!$scope.$$phase) {
              $scope.$apply();
            }
          })
          .$then(function(res) {
            $scope.vertices = Vertex.get({graphId: $routeParams.graphId});
          });
        };

        $scope.reloadData();

        $scope.$watch('gridOptions.filterOptions', function(newVal, oldVal) {
          if (newVal !== oldVal && newVal.currentPage !== oldVal.currentPage) {
            $scope.reloadData();
          }
        }, true);
        $scope.$watch('gridOptions.pagingOptions', function(newVal, oldVal) {
          if (newVal !== oldVal && newVal.currentPage !== oldVal.currentPage) {
            $location.search('currentPage', newVal.currentPage);
            $scope.reloadData();
          }
        }, true);
        $scope.$watch('gridOptions.sortInfo', function(newVal, oldVal) {
          if (newVal !== oldVal) {
            $location.search('sortField', newVal.fields[0]);
            $location.search('sortDirection', newVal.directions[0]);
            $scope.reloadData();
          }
        }, true);

        $scope.isDeleteDisabled = true;
        $scope.$watch('gridOptions.selectedItems', function(newVal, oldVal) {
          if (newVal !== oldVal) {
            $scope.isDeleteDisabled = (newVal.length === 0);
          }
        }, true);

        $scope.deleteSelectedEdges = function() {
            $scope.gridOptions.selectedItems.forEach(function(edge) {
                Edge.delete({graphId: $scope.graphId, edgeId: edge._id}, function() {
                    $scope.getData();
                });
            });
            $scope.isDeleteDisabled = true;
        };
    }).
    controller('EdgeDetailCtrl', function($scope, $routeParams, $location, Edge, Vertex) {
        $scope.graphId = $routeParams.graphId;
        $scope.edgeId = $routeParams.edgeId;

        // retrieve the edge and both vertices
        Edge.get({graphId: $routeParams.graphId, edgeId: $scope.edgeId})
            .$then(function(res) {
              $scope.query = res.resource;
              $scope.vertexIn = Vertex.get({graphId: $routeParams.graphId, vertexId: $scope.query.results._inV});
              $scope.vertexOut = Vertex.get({graphId: $routeParams.graphId, vertexId: $scope.query.results._outV});
            });

        $scope.editEdge = function() {
          $location.path('graphs/' + $scope.graphId + '/edges/' + $scope.query.results._id + '/edit_edge');
        };

        $scope.delete = function() {
            Edge.delete({graphId: $scope.graphId, edgeId: $scope.edgeId}, function() {
                $location.path('graphs/' + $scope.graphId + '/edges');
            });
        }
    }).
    controller('EdgeCreateCtrl', function($scope, $routeParams, $location, Edge, Vertex) {
        $scope.graphId = $routeParams.graphId;
        $scope.query = Vertex.list({graphId: $scope.graphId});
        if ($routeParams.vertexId != undefined) {
          $scope.vertexId = $routeParams.vertexId;
          $scope.edge = new Edge();
          $scope.edge._inV = $scope.vertexId;
        }

        $scope.save = function() {
            Edge.save({graphId: $scope.graphId, inV: $scope.edge._inV}, $scope.edge, function() {
                $location.path('graphs/' + $scope.graphId + '/edges');
            });
        };
    }).
    controller('EdgeEditCtrl', function($scope, $routeParams, $location, User, Edge, Vertex) {
        $scope.graphId = $routeParams.graphId;
        $scope.edgeId = $routeParams.edgeId;
        $scope.query = Vertex.list({graphId: $scope.graphId});
        $scope.query_edge =
            Edge.get({graphId: $scope.graphId, edgeId: $scope.edgeId})
                .$then(function(res) {
                  $scope.edge = res.resource.results;
                });

        //TODO: Edge.PUT is not persisting data to backend
        //TODO:   log(edge) shows correct input; response(edge) returns old/unmodified edge
        $scope.save = function() {
            console.log($scope.edge);
            Edge.update({graphId: $scope.graphId, edgeId: $scope.edgeId}, $scope.edge, function(data) {
                console.log(data);
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
                    $scope.reloadGraph();
                } else {
                    $scope.uploadMessage = "upload failed: " + content.msg;
                }
            }
        }
    }).
    controller('VizHistogramCtrl', function($scope, $location, Histogram) {
      $scope.visualize = function() {
        Histogram.display($scope.query, $scope.facet);
      };
    });
