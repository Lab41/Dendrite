
/**
 * Copyright 2014 In-Q-Tel/Lab41
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
    controller('navCtrl', ['$rootScope', '$scope', '$location', 'User', 'Graph', function ($rootScope, $scope, $location, User, Graph) {
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
            $rootScope.$broadcast('event:loggedIn');
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
    controller('LoginCtrl', function($rootScope, $scope, $location, User) {
      $scope.login = function() {
        User.login($scope.username, $scope.password).
          success(function(){
            $scope.User = User;
            $rootScope.$broadcast('event:loggedIn');
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
    controller('ProjectListCtrl', function($scope, $modal, $location, User, Project) {
        $scope.User = User;
        $scope.query = Project.index();

        // update list of current projects (for create/delete functions)
        $scope.$on('event:reloadProjectNeeded', function() {
          $scope.query = Project.index();
        });

        $scope.createProject = function() {
          $location.path('projects/create');
        };
        $scope.showProject = function(id) {
          $location.path('projects/'+id);
        };
    }).
    controller('ProjectCreateCtrl', function($rootScope, $scope, $location, User, Project, History) {
        $scope.User = User;
        $scope.save = function() {
            Project.save({}, $scope.project)
                    .$then(function(response) {
                      var project = response.data.project;
                      $rootScope.$broadcast('event:reloadProjectNeeded');
                      History.createDir(project._id);
                      $location.path('projects/' + project._id);
                    });
        };
    }).
    controller('ProjectCarveCtrl', function($rootScope, $scope, $route, $location, Project) {
        $scope.projectCarve = function(projectId) {
            var params = {name: $scope.newProjectName, query: $scope.gridOptions.filterOptions.filterText, steps: $scope.newProjectSteps};
            Project.carveSubgraph({projectId: projectId}, params)
                    .$then(function(response) {
                        angular.element('.modal-backdrop').hide();
                        $location.path('projects/' + response.data.projectId);
                    });
        };
    }).
    controller('ProjectDetailCtrl', function($rootScope, $modal, $scope, $timeout, $routeParams, $route, $location, $q, appConfig, Project, Graph, GraphTransform) {
        $scope.projectId = $routeParams.projectId;
        $scope.historyEnabled = appConfig.historyServer.enabled;
        $scope.projectHasData = false;
        $scope.$on('event:projectHasData', function() {
          $scope.projectHasData = true;
        });

        $scope.panelFullScreen = function(title, url) {
          $scope.modalUrl = url;
          $scope.modalTitle = title;
                $scope.safeApply(function() {
                    $modal({
                        scope: $scope,
                        template: "partials/layouts/panel-modal.html",
                        modalClass: 'modal-large',
                        backdrop: 'static'
                    })
                });

        };

        $scope.panelEditable = function() {
          if ($scope.panelEdit) {
            return "panel-editable";
          }
        };

        // boolean function to determine whether to show tabbed visualization panel
        $scope.showTabs = function() {
          return ($scope.projectHasData && $scope.graphLoaded);
        };

        Project.query({projectId: $routeParams.projectId})
                .$then(function(response) {
                    $scope.project = response.data.project;
                    $scope.graphId = $scope.project.current_graph;
                    pollBranches();
                });


        // poll for branches
        //    determine if current branch's graph conflicts with other graphs
        //    (indicates branch has not yet switched/cloned)
        //    FIXME: placeholder until backend lineage between branches complete
        //    FIXME: need comparison: currentBranch.graph != currentBranch.parentBranch.graph
        //    FIXME: alternative comparison: currentGraph.branch != currentGraph.ParentGraph.branch
        var pollBranches = function() {
          $scope.graphLoaded = false;
          Project.branches({projectId: $routeParams.projectId})
                .$then(function(responseAllBranches) {

                    // calculate number of branches that point to current graph
                    var numGraphInstances = 0;
                    Array().forEach.call(responseAllBranches.data.branches, function(branch) {
                      if ($scope.graphId === branch.graphId) {
                        ++numGraphInstances;
                      }
                    });

                    // if current branch's graph is unique, reload everything and
                    // signal presentation layer
                    if (numGraphInstances > 1) {
                      $timeout(pollBranches, appConfig.branches.metadata.pollTimeout);
                    }
                    else {
                      $scope.graphLoaded = true;
                      $scope.graph = Graph.get({graphId: $scope.graphId});
                      $scope.$broadcast('event:reloadGraph');
                    }
                });
        }

        // enable other controllers to call poll function
        $scope.$on("event:pollBranches", function() {
          pollBranches();
        });

        // tripwire to reload current graph
        $scope.$on('event:reloadGraph', function() {
          $scope.forceDirectedGraphData = GraphTransform.reloadRandomGraph($scope.graphId);
        });

        // get project's branches
        $scope.queryCurrentBranch = Project.currentBranch({projectId: $routeParams.projectId});
        $scope.queryBranches = Project.branches({projectId: $routeParams.projectId});

        // get the projects branches
        $scope.$on('event:reloadProjectNeeded', function() {
          Project.currentBranch({projectId: $routeParams.projectId})
                  .$then(function(response) {
                    $scope.queryCurrentBranch = response.data;
                    $scope.graphId = $scope.queryCurrentBranch.branch.graphId;
                    $scope.$broadcast('event:reloadGraph');
                    $scope.$broadcast('event:pollBranches');
                  });
          $scope.queryBranches = Project.branches({projectId: $routeParams.projectId});
        });

        $scope.deleteItem = function(item){
          Project.delete({projectId: item._id}).
            $then(function(response) {
              var data = response.data;
              if (data.msg === "deleted") {
                $rootScope.$broadcast('event:reloadProjectNeeded');
                $location.path('projects');
              }
            });
        };
    }).
    controller('GraphDetailCtrl', function($scope, $routeParams, $q, User, Graph, GraphTransform) {
        $scope.User = User;
        $scope.graphId = $routeParams.graphId;
        $scope.graph = Graph.get({graphId: $routeParams.graphId});

        // This can be removed after we upgrade to angular 1.2.
        //$scope.forceDirectedGraphData = {
        //  vertices: Vertex.query({graphId: $scope.graphId}),
        //  edges: Edge.query({graphId: $scope.graphId})
        //};
        $scope.forceDirectedGraphData = GraphTransform.reloadRandomGraph($scope.graphId);
        $scope.$on('event:reloadGraph', function() {
          $scope.forceDirectedGraphData = GraphTransform.reloadRandomGraph($scope.graphId);
        });
    }).
    controller('GraphSaveCtrl', function ($scope, $routeParams, $http, GraphTransform) {
        $scope.fileSaved = false;
        $scope.fileSaving = false;

        $scope.saveFile = function() {
            $scope.fileSaving = true;
            GraphTransform.saveFile($scope.graphId, $scope.projectId, this.format)
                .success(function(){
                    $scope.fileSaving = false;
                    $scope.fileSaved = true;
                    $scope.savedMessage = "Graph "+$scope.graphId+" checkpointed";
                })
                .error(function(response){
                    $scope.fileSaved = true;
                    $scope.fileSaving = false;
                    $scope.savedMessage = "upload failed!";
                });
        };
    }).
    controller('AnalyticsDetailCtrl', function($scope, $location, $routeParams, $filter, $q, appConfig, User, Vertex, Edge, Analytics, Helpers, $timeout) {
        // config
        $scope.activeAnalytics = [];
        $scope.colorProgressBars = Helpers.colorProgressBars;

        // periodically poll for active calculations
        var pollActive = function() {
          Analytics.getJob({jobId: $routeParams.analyticsId})
                      .$then(function(data) {
                          $scope.analytic = data.data.job;
                          if ($scope.analytic !== undefined && $scope.analytic.progress < 1.0) {
                            $timeout(pollActive, appConfig.analytics.metadata.pollTimeout);
                          }
                      });
        }
        pollActive();

        // Delete job
        $scope.deleteJob = function(id){
          Analytics.deleteJob({jobId: id})
            .$then(function(response) {
              var data = response.data;
              if (data.msg === "deleted") {
                window.history.back();
              }
            });
        };


    }).
    controller('BranchEditCtrl', function($scope, $location, $modal, $routeParams, Branch, Project) {

        // modals
        $scope.branchSwitch = true;
        $scope.showModalCreate = function(branch) {
          $scope.pivotBranch = branch;
          $modal({scope: $scope, template: 'partials/branches/create.html'});
        };

        // create a new branch
        $scope.createBranch = function() {
          $scope.branchMessage = undefined;
          $scope.branchError = undefined;

          // check for name conflict
          Project.getBranch({projectId: $routeParams.projectId, branchName: $scope.branchName},

            // notify on name conflict
            function(data) {
              $scope.branchError = "A branch already exists with name: "+$scope.branchName;
            },

            // create new branch
            function(error) {
              Project.createBranch({projectId: $routeParams.projectId, branchName: $scope.branchName}, {graphId: $scope.pivotBranch.graphId})
                      .$then(function(response) {

                          // notify
                          $scope.branchMessage = "Created branch: "+$scope.branchName;
                          $scope.safeApply();
                          if ($scope.branchSwitch) {
                            Project.switchBranch({projectId: $routeParams.projectId}, {branchName: $scope.branchName})
                                    .$then(function(response) {
                                      $scope.$emit('event:reloadProjectNeeded');
                                    });
                          }
                          else {
                            $scope.$emit('event:reloadProjectNeeded');
                          }

                      });
            });

        };

        // delete a branch
        $scope.deleteBranch = function(branch) {
          Branch.delete({branchId: branch._id})
                  .$then(function(data) {
                      $scope.queryBranches = Project.branches({projectId: $routeParams.projectId});
                      $scope.branchMessage = "Deleted branch "+branch.name;
                      $scope.safeApply();
                  });
        };

        // switch to a different branch
        $scope.switchBranch = function(branch) {
          Project.switchBranch({projectId: $routeParams.projectId}, {branchName: branch.name})
                  .$then(function(response) {
                    $scope.branchMessage = "Now using branch: "+branch.name;
                    $scope.$emit('event:reloadProjectNeeded');
                  });
        };

        // commit the branch
        $scope.commitBranch = function(branch) {
          Project.commitBranch({projectId: $routeParams.projectId}, undefined)
                  .$then(function(response) {
                    $scope.branchMessage = "Committed branch: "+branch.name;
                    $scope.$emit('event:reloadProjectNeeded');
                  });
        };
    }).
    controller('AnalyticsFormCtrl', function($rootScope, $scope, $location, $routeParams, $filter, $q, appConfig, User, Vertex, Edge, Analytics, Helpers, $timeout) {
        // placeholder default attributes
        $scope.graphId = $routeParams.graphId;
        $scope.algorithms = appConfig.algorithms;

        $scope.setAnalyticType = function(t) {
          $scope.analyticType = t;
        };

        $scope.$watch('analyticType', function (newVal, oldVal) {
            if (newVal !== oldVal && newVal !== undefined) {
              // perform deep copy to avoid changing default values
              $scope.attr = {};
              angular.copy(appConfig.algorithms[$scope.analyticType].defaults, $scope.attr);

              if (newVal === 'sssp') {
                $scope.allVertices = Vertex.list({graphId: $scope.graphId});
              }
            }
        }, true);

        $scope.isAnalyticType = function(t) {
          return (appConfig.algorithms[$scope.analyticType] !== undefined && t === appConfig.algorithms[$scope.analyticType].category);
        };

        // calculate analytic job
        $scope.calculate = function() {
          // Barycenter
          if ($scope.analyticType === "barycenterDistance") {
            Analytics.createJungBarycenterDistance({graphId: $routeParams.graphId}, undefined);
          }
          // BetweennessCentrality
          else if ($scope.analyticType === "betweennessCentrality") {
            Analytics.createJungBetweennessCentrality({graphId: $routeParams.graphId}, undefined);
          }
          // ClosenessCentrality
          else if ($scope.analyticType === "closenessCentrality") {
            Analytics.createJungClosenessCentrality({graphId: $routeParams.graphId}, undefined);
          }
          // Eigenvector
          else if ($scope.analyticType === "eigenvectorCentrality") {
            Analytics.createJungEigenvectorCentrality({graphId: $routeParams.graphId}, undefined);
          }
          // PageRank
          else if ($scope.analyticType === "pagerank") {
            if ($scope.attr.analyticEngine === "jung") {
              Analytics.createJungPageRank({graphId: $routeParams.graphId}, {alpha: 1-$scope.attr.dampingFactor});
            }
            else {
              Analytics.createGraphLab({graphId: $routeParams.graphId, algorithm: $scope.analyticType}, undefined);
            }
          }
          // Total Subgraph Communicability
          else if ($scope.analyticType === "TSC") {
            Analytics.createGraphLab({graphId: $routeParams.graphId, algorithm: $scope.analyticType}, undefined);
          }
          // Single source shortest path
          else if ($scope.analyticType === "sssp") {
            Analytics.createGraphLab({graphId: $routeParams.graphId, algorithm: $scope.analyticType, sourceVertex: $scope.sourceVertex}, undefined);
          }
          // connected component
          else if ($scope.analyticType === "connected_component") {
            Analytics.createGraphLab({graphId: $routeParams.graphId, algorithm: $scope.analyticType}, undefined);
          }
          // connected component histogram
          else if ($scope.analyticType === "connected_component_stats") {
            Analytics.createGraphLab({graphId: $routeParams.graphId, algorithm: $scope.analyticType}, undefined);
          }
          // graph coloring
          else if ($scope.analyticType === "simple_coloring") {
            Analytics.createGraphLab({graphId: $routeParams.graphId, algorithm: $scope.analyticType}, undefined);
          }
          // SNAP
          else if ($scope.analyticType === "snapCentrality") {
            Analytics.createSnap({graphId: $routeParams.graphId, algorithm: "centrality"}, undefined);
          }
          // Snap
          else if ($scope.analyticType === "Snap") {
            Analytics.createSnap({graphId: $routeParams.graphId, algorithm: $scope.attr.algorithm}, undefined);
          }
          // Edge Degrees
          else if ($scope.analyticType === "edgeDegrees") {
            if ($scope.attr.analyticEngine === "faunus") {
              Analytics.createEdgeDegreesFaunus({graphId: $routeParams.graphId}, undefined);
            }
            else if ($scope.attr.analyticEngine === "titan") {
              Analytics.createEdgeDegreesTitan({graphId: $routeParams.graphId}, undefined);
            }
          }

          $rootScope.$broadcast("event:pollActiveAnalytics");
        };
    }).
    controller('AnalyticsListCtrl', function($rootScope, $scope, $location, $routeParams, $filter, $q, appConfig, Project, Graph, Analytics, Helpers, $timeout) {
        // config
        $scope.activeAnalytics = [];
        $scope.colorProgressBars = Helpers.colorProgressBars;

        // show result
        $scope.showAnalytic = function(id) {
          $location.path('graphs/' + $routeParams.graphId + '/analytics/' + id);
        };

        $scope.setNumJobsTotal = function(val) {
          $scope.$parent.numJobsTotal = val;
        };
        $scope.setNumJobsTotal(0);

        $scope.setNumJobsPending = function(val) {
          $scope.$parent.numJobsPending = val;
        };
        $scope.setNumJobsPending(0);

        $scope.setNumJobsError = function(val) {
          $scope.$parent.numJobsError = val;
        };
        $scope.setNumJobsError(0);

        $scope.deleteAnalytic = function(job) {
          Analytics.deleteJob({jobId: job._id})
                    .$then(function(data) {
                        var update = $scope.activeAnalytics.jobs.filter(function(el) {
                          return el._id !== job._id
                        });
                        $scope.activeAnalytics.jobs = update;
                        pollActive();
                    });
        }

        // periodically poll for active calculations
        // use $then syntax to avoid full list refresh each time
        var pollActive = function() {
          var numJobsPending = 0;
          var numJobsError = 0;
          var numJobsComplete = 0;
          var activeAnalytics = {jobs: []};
          Graph.get({graphId: $routeParams.graphId})
                .$then(function(dataGraph) {
                    Project.jobs({projectId: dataGraph.data.graph.projectId})
                            .$then(function(dataJobs) {

                                // determine if client should continue polling
                                // if not, will poll on next event:pollActiveAnalytics
                                var pollAgain = false;
                                Array().forEach.call(dataJobs.data.jobs, function(job) {

                                  if (job.parentJob === undefined) {
                                    activeAnalytics.jobs.push(job);

                                    if (job.progress < 1.0) {

                                      // check for errored and pending jobs
                                      if (job.state === "ERROR") {
                                        numJobsError++;
                                      }
                                      else {
                                        pollAgain = true;
                                        numJobsPending++;
                                      }
                                    }
                                  }
                                });

                                // set list of active jobs
                                $scope.activeAnalytics = activeAnalytics;
                                $scope.setNumJobsTotal(activeAnalytics.jobs.length);

                                // re-poll for analytics or let app know
                                if (pollAgain) {
                                  $timeout(pollActive, appConfig.analytics.metadata.pollTimeout*5);
                                }
                                else {

                                  // alert app to change only if job(s) have moved from pending->complete
                                  if (numJobsPending !== $scope.numJobsPending && $scope.numJobsError === numJobsError) {
                                    $rootScope.$broadcast('event:reloadProjectNeeded');
                                  }
                                }

                                // update number of jobs
                                $scope.setNumJobsPending(numJobsPending);
                                $scope.setNumJobsError(numJobsError);
                            });
                });

        }

        // enable other controllers to call poll function
        $scope.$on("event:pollActiveAnalytics", function() {
          pollActive();
        });

        // poll on page entry
        pollActive();
    }).
    controller('VertexListCtrl', function($scope, $location, $timeout, $routeParams, $filter, $q, appConfig, User, Graph, Project, Vertex, Edge, ElasticSearch) {

      $scope.graphId = $routeParams.graphId;
      $scope.isCollapsed = true;
      $scope.selectedItems = [];
      $scope.queryStyle = "vertices";
      $scope.vertexFrom = "";
      Graph.get({graphId: $routeParams.graphId})
            .$then(function(dataGraph) {
                $scope.queryProject = Project.get({projectId: dataGraph.data.graph.projectId});
            });

      $scope.$on('event:reloadProjectNeeded', function() {
        $scope.refresh();
      });

      $scope.collapseJobs = function() {
        return $scope.isCollapsed;
      };

      $scope.followEdges = function() {
        $routeParams.mode = "edge";
        $scope.queryStyle = "edges";
        $scope.refresh();
        return false; // prevent normal link behavior
      };

      $scope.followInEdges = function() {
        $scope.queryStyle = "in-edges";
        $scope.refresh();
        return false; // prevent normal link behavior
      };

      $scope.followOutEdges = function() {
        $scope.queryStyle = "out-edges";
        $scope.refresh();
        return false; // prevent normal link behavior
      };

      $scope.addVertexEdge = function() {
        $location.path('graphs/' + $scope.graphId + '/create_edge/' + $scope.selectedItems[0]._id);
      };

      $scope.editVertex = function() {
        $location.path('graphs/' + $scope.graphId + '/vertices/' + $scope.selectedItems[0]._id + '/edit_vertex');
      };

      $scope.dynamicWidth = function() {
        return ($scope.hasSelectedItems) ? 'span7' : 'span11';
      };

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
          $scope.refresh();
        });

        // Make sure to clear the selected list of items.
        $scope.selectedItems.length = 0;
      };

      // reset selectedItems array
      $scope.resetItems = function() {
        while($scope.selectedItems.length>0) {
          $scope.selectedItems.shift();
        }
        $scope.refresh();
      }

      $scope.objectType = "vertex";
      $scope.items = [];

      // filter
      $scope.filterOptions = {
          filterText: "",
          useExternalFilter: true
      };

      // paging
      $scope.totalServerItems = 0;
      $scope.pagingOptions = {
          pageSizes: [5, 10, 25, 50, 100],
          pageSize: appConfig.elasticSearch.fieldSize,
          currentPage: 1
      };

      // sort
      $scope.sortedField = {};
      $scope.sortOptions = {
          fields: [],
          directions: []
      };

      // call elasticSearch sort when header is clicked
      // extra function needed to be able to dynamically set
      // sortOptions without a $scope.$watch, which would
      // re-trigger refresh() call in an endless loop
      $scope.externalSort = function(field, direction) {

        // default the sortedField to "asc" unless already set to "asc"
        var newSortedField = {};
        newSortedField[field] = ($scope.sortedField && $scope.sortedField[field] === "asc") ? "desc" : "asc";
        $scope.sortedField = newSortedField;

        // update list
        $scope.elasticSearchAction = "Sorting";
        $scope.refresh();
      }

      $scope.formatSelectedNames = function() {
        var names = "";
        if ($scope.selectedItems.length == 1) {
          names = $filter('capitalize')($scope.selectedItems[0].name);
        } else if ($scope.selectedItems.length > 1) {
          names = $filter('capitalize')($scope.selectedItems[0].name) + ", " + $filter('capitalize')($scope.selectedItems[1].name);
          if ($scope.selectedItems.length > 1) {
            names += ", ...";
          }
        }
        return names;
      };

      $scope.selectedOne = function() {
        return $scope.selectedItems.length===1
      };

      $scope.selectedMany = function() {
        return $scope.selectedItems.length>1
      };

      // grid
      $scope.gridOptions = {
          data: "items",
          columnDefs: 'columnDefs',
          enablePaging: true,
          enablePinning: true,
          pagingOptions: $scope.pagingOptions,
          filterOptions: $scope.filterOptions,
          keepLastSelected: true,
          multiSelect: true,
          showColumnMenu: true,
          showFilter: true,
          showGroupPanel: true,
          showFooter: true,
          selectedItems: $scope.selectedItems,
          selectWithCheckboxOnly: false,
          showSelectionCheckbox: true,
          sortInfo: $scope.sortOptions,
          totalServerItems: "totalServerItems",
          useExternalSorting: true,
          plugins: [new ngGridFlexibleHeightPlugin()],
          i18n: "en"
      };

      $scope.reload = function(elasticSearchAction) {
          $scope.resetItems();
          $scope.queryStyle = "vertices";
          $scope.vertexFrom = "";
          $scope.refresh(elasticSearchAction);
      }

      $scope.refresh = function(elasticSearchAction) {
        if ($scope.queryStyle === "vertices") {

          if (elasticSearchAction !== undefined){
            $scope.elasticSearchAction = elasticSearchAction;
          }
          $scope.searching = true;

          // first clear current items to give visual indicator
          $scope.items = [];
          if (!$scope.$$phase) {
              $scope.$apply();
          }

          var elasticSortOptions = [];
          Object.keys($scope.sortedField).forEach(function(k) {
            var newHash = {};
            newHash[k] = { "order" : $scope.sortedField[k] };
            elasticSortOptions.push( newHash );
          });

          var p = {
              queryTerm: $scope.filterOptions.filterText,
              pageNumber: $scope.pagingOptions.currentPage,
              pageSize: $scope.pagingOptions.pageSize,
              sortInfo: elasticSortOptions,
              resultType: $scope.objectType
          };

          ElasticSearch.search(p)
            .success(function(data, status, headers, config) {
                $scope.totalServerItems = data.hits.total;

                // update sort options
                var sortFields = [];
                var sortDirections = [];

                // build array of results
                var resultArray = [];
                var resultKeys = {};
                data.hits.hits.forEach(function(hit) {
                  if (hit._type === $scope.objectType) {
                    hit._source._id = hit._source._vertexId;
                    resultArray.push(hit._source);

                    // extract all keys (to dynamically update table columns)
                    Object.keys(hit._source).forEach(function(k) {
                      if (k !== ($scope.objectType+'Id') && k !== "_id" && k !== "_vertexId") {
                        resultKeys[k] = true;
                      }
                    });
                  }
                });

                // create table columns from result index keys
                // headerCellTemplate adds externalSort() function
                // to allow dynamic setting of columns based on results
                $scope.columnDefs = [];
                Object.keys(resultKeys).forEach(function(k) {
                  $scope.columnDefs.push({
                    field: k,
                    displayName: k,
                    enableCellEdit: false,
                    headerCellTemplate: '<div class="ngHeaderSortColumn  ngSorted" ng-style="{\'cursor\': col.cursor}" ng-class="{ \'ngSorted\': !noSortVisible }" style="cursor: pointer;" draggable="true">\
                      <div ng-click="externalSort(col.field)" ng-class="\'colt\' + col.index" class="ngHeaderText"">{{col.displayName}}</div>\
                    </div>'
                  });

                  // update the table column heads
                  // use default sort unless set in external query
                  sortFields.push(k);
                  if ($scope.sortedField) {
                    if (k in $scope.sortedField) {
                      sortDirections.push($scope.sortedField[k]);
                    }
                    else {
                      sortDirections.push(appConfig.elasticSearch.sorting.direction);
                    }
                  }
                });

                $scope.columnDefs.sort(function(a, b) {
                  if (a.field < b.field) { return -1; }
                  else if (a.field > b.field) { return 1; }
                  else { return 0; }
                });

                // update sort options
                $scope.sortOptions.fields = sortFields;
                $scope.sortOptions.directions = sortDirections;

                // store the results
                $scope.items = resultArray;
                $scope.searching = false;

            }).error(function(data, status, headers, config) {
                //alert(JSON.stringify(data));
          });
        }
        else {
          $q.all(
            $scope.selectedItems.map(function(vertex) {
              var deferred = $q.defer();
              var f;
              if ($scope.queryStyle === "edges") {
                f =  Vertex.queryConnectedVertices;
              } else if ($scope.queryStyle === "in-edges") {
                f =  Vertex.queryConnectedInVertices;
              } else if ($scope.queryStyle === "out-edges") {
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
              if (query != undefined) {
                query.results.forEach(function(vertex) {
                  if (vertexExists[vertex._id] === undefined) {
                    vertexExists[vertex._id] = vertex._id;
                    results.push(vertex);
                  }
                });
              }
              $scope.items = results;
              $scope.totalServerItems = results.length;
              $scope.vertexFrom = $scope.selectedItems[0];
          });
        }
      };

      // watches
      $scope.$watch('pagingOptions', function (newVal, oldVal) {
          if (newVal !== oldVal) {
              $scope.elasticSearchAction = "Paging";
              $scope.refresh();
          }
      }, true);

      $scope.$watch('filterOptions', function (newVal, oldVal) {
          if (newVal !== oldVal) {
              $scope.elasticSearchAction = "Searching";
              $scope.refresh();
          }
      }, true);

      // Update the hasSelectedItems when selectedItem changes.
      $scope.$watch('selectedItems', function(newVal, oldVal) {
        if (newVal !== oldVal) {
          $scope.hasSelectedItems = ($scope.selectedItems.length !== 0);
          $scope.selectedVertex = $scope.selectedItems[0];

          // redraw the grid
          // use timeout to appear more responsive than external resize event
          //$('.grid-container').trigger('resize');
          $timeout(function() {
            $scope.gridOptions.$gridServices.DomUtilityService.RebuildGrid($scope.gridOptions.$gridScope, $scope.gridOptions.ngGrid);
          }, 0);
        }


      }, true);


      // load data on enter
      $scope.refresh();

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
            Vertex.save({graphId: $scope.graphId}, $scope.vertex)
                  .$then(function(data) {
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
            Vertex.update({graphId: $scope.graphId, vertexId: $scope.vertexId}, $scope.vertex)
                  .$then(function(data) {
                    $location.path('graphs/' + $scope.graphId + '/vertices');
                  });
        };
    }).
    controller('EdgeListCtrl', function($scope, $location, $routeParams, $filter, $q, appConfig, User, Graph, Project, Vertex, Edge, ElasticSearch) {

      $scope.graphId = $routeParams.graphId;
      $scope.selectedItems = [];
      $scope.queryStyle = "edges";
      Graph.get({graphId: $routeParams.graphId})
            .$then(function(dataGraph) {
                $scope.queryProject = Project.get({projectId: dataGraph.data.graph.projectId});
            });

      $scope.editEdge = function() {
        $location.path('graphs/' + $scope.graphId + '/edges/' + $scope.selectedItems[0]._id + '/edit_edge');
      };

      // Delete the selected items.
      $scope.deleteSelectedItems = function() {
        $q.all(
          $scope.selectedItems.map(function(edge) {
            var deferred = $q.defer();
            Edge.delete({graphId: $scope.graphId, edgeId: edge._id}, function() {
              deferred.resolve();
            }, function() {
              deferred.reject();
            });
            return deferred.promise;
          })
        ).then(function() {
          // Refresh the data once all the items are deleted.
          $scope.refresh();
        });

        // Make sure to clear the selected list of items.
        $scope.selectedItems.length = 0;
      };

      // reset selectedItems array
      $scope.resetItems = function() {
        while($scope.selectedItems.length>0) {
          $scope.selectedItems.shift();
        }
        $scope.refresh();
      }

      $scope.objectType = "edge";
      $scope.items = [];

      // filter
      $scope.filterOptions = {
          filterText: "",
          useExternalFilter: true
      };

      // paging
      $scope.totalServerItems = 0;
      $scope.pagingOptions = {
          pageSizes: [5, 10, 25, 50, 100],
          pageSize: appConfig.elasticSearch.fieldSize,
          currentPage: 1
      };

      // sort
      $scope.sortedField = {};
      $scope.sortOptions = {
          fields: [],
          directions: []
      };

      // call elasticSearch sort when header is clicked
      // extra function needed to be able to dynamically set
      // sortOptions without a $scope.$watch, which would
      // re-trigger refresh() call in an endless loop
      $scope.externalSort = function(field, direction) {
        // default the sortedField to "asc" unless already set to "asc"
        var newSortedField = {};
        newSortedField[field] = ($scope.sortedField && $scope.sortedField[field] === "asc") ? "desc" : "asc";
        $scope.sortedField = newSortedField;

        // update list
        $scope.elasticSearchAction = "Sorting";
        $scope.refresh();
      }

      $scope.formatSelectedNames = function() {
        var names = "";
        if ($scope.selectedItems.length == 1) {
          names = $scope.selectedItems[0]._inV+'-'+$scope.selectedItems[0]._label+'-'+$scope.selectedItems[0]._outV;
        } else if ($scope.selectedItems.length > 1) {
          names = $scope.selectedItems.length+' edges';
        }
        return names;
      };

      $scope.selectedOne = function() {
        return $scope.selectedItems.length===1
      };

      $scope.selectedMany = function() {
        return $scope.selectedItems.length>1
      };

      // grid
      $scope.gridOptions = {
          data: "items",
          columnDefs: 'columnDefs',
          enablePaging: true,
          enablePinning: true,
          pagingOptions: $scope.pagingOptions,
          filterOptions: { filterText: ''},//TODO: enable backend filtering once ES indexes edge details
          keepLastSelected: true,
          multiSelect: true,
          showColumnMenu: true,
          showFilter: true,
          showGroupPanel: true,
          showFooter: true,
          selectedItems: $scope.selectedItems,
          selectWithCheckboxOnly: false,
          showSelectionCheckbox: true,
          sortInfo: $scope.sortOptions,
          totalServerItems: "totalServerItems",
          useExternalSorting: false, //TODO: enable external sorting once ES indexes edge details
          enableSorting: true,
          plugins: [new ngGridFlexibleHeightPlugin()],
          i18n: "en"
      };

      $scope.reload = function(elasticSearchAction) {
          $scope.resetItems();
          $scope.queryStyle = "edges";
          $scope.refresh(elasticSearchAction);
      }

      $scope.refresh = function(elasticSearchAction) {

        if ($scope.queryStyle === "edges") {
          console.log('refreshing');

          if (elasticSearchAction !== undefined){
            $scope.elasticSearchAction = elasticSearchAction;
          }
          $scope.searching = true;

          // first clear current items to give visual indicator
          $scope.items = [];
          if (!$scope.$$phase) {
              $scope.$apply();
          }

          var elasticSortOptions = [];
          Object.keys($scope.sortedField).forEach(function(k) {
            var newHash = {};
            newHash[k] = { "order" : $scope.sortedField[k] };
            elasticSortOptions.push( newHash );
          });

          var p = {
              queryTerm: $scope.filterOptions.filterText,
              pageNumber: $scope.pagingOptions.currentPage,
              pageSize: $scope.pagingOptions.pageSize,
              sortInfo: elasticSortOptions,
              resultType: $scope.objectType
          };

          var ignoredKeys = [$scope.objectType+'Id', '_'+$scope.objectType+'Id', "_id", "_type"];

          ElasticSearch.search(p)
            .success(function(data, status, headers, config) {
                $scope.totalServerItems = data.hits.total;

                // update sort options
                var sortFields = [];
                var sortDirections = [];

                // build array of results
                var resultArray = [];
                var resultKeys = {};
                data.hits.hits.forEach(function(hit) {
                  if (hit._type === $scope.objectType) {

                      Edge.query({graphId: $scope.graphId, edgeId: hit._id})
                          .$then(function(dataEdge) {

                              // extract information on inbound Vertices
                              Vertex.query({graphId: $scope.graphId, vertexId: dataEdge.data.results._inV})
                                    .$then(function(dataVertex) {
                                        dataEdge.data.results._inV = dataVertex.data.results.name;
                                    });

                              // extract information on outbound Vertices
                              Vertex.query({graphId: $scope.graphId, vertexId: dataEdge.data.results._outV})
                                    .$then(function(dataVertex) {
                                        dataEdge.data.results._outV = dataVertex.data.results.name;
                                    });

                              // build results
                              resultArray.push(dataEdge.data.results);


                              // if all results processed, update view
                              if (resultArray.length === data.hits.hits.length) {

                                // extract all keys (to dynamically update table columns)
                                Object.keys(dataEdge.data.results).forEach(function(k) {
                                  if (ignoredKeys.indexOf(k) == -1) {
                                    resultKeys[k] = true;
                                  }
                                });


                                // create table columns from result index keys
                                // headerCellTemplate adds externalSort() function
                                // to allow dynamic setting of columns based on results
                                $scope.columnDefs = [];
                                Object.keys(resultKeys).forEach(function(k) {
                                  $scope.columnDefs.push({
                                    field: k,
                                    displayName: k,
                                    enableCellEdit: false,
                                    headerCellTemplate: '<div class="ngHeaderSortColumn  ngSorted" ng-style="{\'cursor\': col.cursor}" ng-class="{ \'ngSorted\': !noSortVisible }" style="cursor: pointer;" draggable="true">\
                                      <div ng-class="\'colt\' + col.index" class="ngHeaderText"">{{col.displayName}}</div>\
                                    </div>'
                                  });

                                  // update the table column heads
                                  // use default sort unless set in external query
                                  sortFields.push(k);
                                  if ($scope.sortedField) {
                                    if (k in $scope.sortedField) {
                                      sortDirections.push($scope.sortedField[k]);
                                    }
                                    else {
                                      sortDirections.push(appConfig.elasticSearch.sorting.direction);
                                    }
                                  }
                                });

                                $scope.columnDefs.sort(function(a, b) {
                                    if (a.field < b.field) { return -1; }
                                    else if (a.field > b.field) { return 1; }
                                    else { return 0; }
                                });

                                // update sort options
                                $scope.sortOptions.fields = sortFields;
                                $scope.sortOptions.directions = sortDirections;

                                // store the results
                                $scope.items = resultArray;
                                $scope.searching = false;
                            }
                          });
                  }
                });
            }).error(function(data, status, headers, config) {
                //alert(JSON.stringify(data));
          });
        }
      };

      // watches
      $scope.$watch('pagingOptions', function (newVal, oldVal) {
          if (newVal !== oldVal) {
              $scope.elasticSearchAction = "Paging";
              $scope.refresh();
          }
      }, true);

//TODO: enable backend filtering once ES indexes edge details
//      $scope.$watch('filterOptions', function (newVal, oldVal) {
//          if (newVal !== oldVal) {
//              $scope.elasticSearchAction = "Searching";
//              $scope.refresh();
//          }
//      }, true);

      // Update the hasSelectedItems when selectedItem changes.
      $scope.$watch('selectedItems', function(newVal, oldVal) {
        if (newVal !== oldVal) {
          $scope.hasSelectedItems = ($scope.selectedItems.length !== 0);
          $scope.selectedVertex = $scope.selectedItems[0];
        }
      }, true);

      // load data on entry
      $scope.refresh();

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
            Edge.save({graphId: $scope.graphId, inV: $scope.edge._inV}, $scope.edge)
                .$then(function(data) {
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
    controller('FileUploadCtrl', function ($rootScope, $scope, $routeParams, $modal, appConfig) {
        $scope.fileUploaded = false;
        $scope.fileUploading = false;
        $scope.indexTypes = [
            "string",
            "text",
            "integer",
            "float",
            "double",
            "geocoordinate"
        ];
        $scope.keysForGraph = [];

        $scope.uploading = function() {
            $scope.fileUploading = true;
        };

        $scope.uploadFilePreview = function() {
            console.log('preview...');
            console.log($scope.filename);
            var reader = new FileReader();
            //reader.readAsDataURL(changeEvent.target.files[0]);
            return false;
        };

        // FIXME: For some reason firefox fires this off on load. So ignore the error for now.
        $scope.actuallyUploading = false;

        $scope.uploadFile = function(content) {
            // See fixme above.
            if (!$scope.actuallyUploading) { return; }

            $scope.actuallyUploading = false;

            $scope.fileUploading = false;
            $scope.fileUploaded = true;
            if (content.status === "ok") {
                $scope.uploadMessage = "file uploaded";
                $scope.$emit('event:reloadGraph');
                $rootScope.$broadcast('event:graphFileImported');
            } else if (content.msg !== undefined) {
                $scope.uploadMessage = "upload failed: " + content.msg;
            } else {
                $scope.uploadMessage = "upload failed";
            }
        };

        // push/slice checkbox from list
        $scope.selectedKeys = {vertices: [], edges: []};
        $scope.arrayShow = {vertices: [], edges: []};

        $scope.toggleSelection = function(key) {
            key.show = !key.show;
        };

        $scope.getSelected = function() { // 'load graph' button calls getSelected()

          Object.keys($scope.selectedKeys).forEach(function(key) { //arrays within selectedKeys
            for (var x = 0; x < $scope.selectedKeys[key].length; x++) { //items in arrays

              if ($scope.selectedKeys[key][x].show) { //if checked in load form, add fields to array for indexing
                delete $scope.selectedKeys[key][x].show;
                $scope.arrayShow[key].push($scope.selectedKeys[key][x]);
              }
            }
          });
            $scope.loadGraph($scope.arrayShow); //loadgraph function being called with selected keys from form
        }

        var restrictedKeys = ["_id", "id", "_type", "_outV", "_inV", "source", "target", "blueprintsId"];

        $scope.$on('event:graphFileParsed', function() {
          $scope.selectedKeys = {vertices: [], edges: []};
          if (!appConfig.fileUpload.parseGraphFile) {
            $scope.safeApply(function() {
              $scope.fileParsed = true;
            });
          }
          else {
            $scope.fileParsed = false;
            if ($scope.keysForGraph.vertices.length > 0) {
              $scope.fileParsed = true;
              $scope.fileParseError = false;
              $scope.keysForGraph.vertices.forEach(function(k) {
                  if (restrictedKeys.indexOf(k) == -1) {
                      $scope.selectedKeys.vertices.push({
                          name: k,
                          type: $scope.indexTypes[0],
                          show: true
                      });
                  }
              });
              $scope.keysForGraph.edges.forEach(function(k) {
                  if (restrictedKeys.indexOf(k) == -1) {
                      $scope.selectedKeys.edges.push({
                          name: k,
                          type: $scope.indexTypes[0],
                          show: true
                      });
                  }
              });

              $scope.safeApply(function() {
                $modal({
                  scope: $scope,
                  template: 'partials/graphs/form-select-keys.html'
                }).then(function() {
                      tourFileSelected();
                });
              });
            } else {
                $scope.safeApply(function() {
                  $scope.fileParseError = "Error: Unrecognized Format!";
                });
            }
          }
        });

        // auto-submit form to upload graph
        // **note: explicitly set searchkeys value since Angular might be pending the scope's data update
        $scope.loadGraph = function(selected) {
          // See fixme above.
          $scope.actuallyUploading = true;

          // build the list of key-val pairs
          console.log($scope.arrayShow.length);
          var selectedKeysJson = JSON.stringify(selected);
          angular.element('#form-file-upload input[name="searchkeys"]').val(selectedKeysJson);

          $scope.safeApply(function() {
            angular.element('#form-file-upload').submit();
          });
        };
    }).
    controller('VizHistogramCtrl', function($scope, $location, Histogram, ElasticSearch, appConfig) {
      $scope.searching = false;

      $scope.$watch('graphId', function(newVal, oldVal) {
          if (newVal !== undefined) {
              ElasticSearch.mapping($scope.graphId)
                  .success(function(data) {
                      if (data.vertex !== undefined && data.vertex.properties !== undefined) {
                        $scope.searchFacets = Object.keys(data.vertex.properties);
                      }
                  });

              $scope.visualize = function() {
                  $scope.searching = true;
                  Histogram.display($scope.graphId, $scope.query, $scope.facet)
                      .success(function() {
                          $scope.searching = false;
                      })
                      .error(function() {
                          $scope.searching = false;
                      });
              };
          }
      });
    }).
    controller('VizMapCtrl', function($scope, $location, Map, ElasticSearch, appConfig) {
      $scope.searching = false;

      $scope.$watch('graphId', function(newVal, oldVal) {
          if (newVal !== undefined) {
              ElasticSearch.mapping($scope.graphId)
                  .success(function(data) {
                      var elasticValueFields = [];
                      Object.keys(data.vertex.properties).forEach(function(k) {
                          var val = data["vertex"]["properties"][k]["type"];
                          if (val === "geo_point") {
                              elasticValueFields.push(k);
                          };
                      });
                      $scope.searchFacets = elasticValueFields;
                  });

              $scope.visualize = function() {
                  $scope.searching = true;
                  Map.display($scope.graphId, $scope.filter, $scope.size, $scope.mapType, $scope.searchFacets)
                      .success(function() {
                          $scope.searching = false;
                      })
                      .error(function() {
                          $scope.searching = false;
                      });
              };
          }
      });
    }).
    controller('VizScatterplotCtrl', function($scope, $location, Scatterplot, ElasticSearch, appConfig) {
      $scope.searching = false;

      $scope.$watch('graphId', function(newVal, oldVal) {
          if (newVal !== undefined) {
              ElasticSearch.mapping($scope.graphId)
                  .success(function(data) {
                      $scope.searchUniqueField = Object.keys(data.vertex.properties);
                  });
              ElasticSearch.mapping($scope.graphId)
                  .success(function(data) {
                      var elasticValueFields = [];
                      Object.keys(data.vertex.properties).forEach(function(k) {
                          var val = data["vertex"]["properties"][k]["type"];
                          if (val === "integer" ||
                              val === "double" ||
                              val === "float") {
                              elasticValueFields.push(k);
                          };
                      });
                      $scope.searchFacets = elasticValueFields;
                  });

              $scope.visualize = function() {
                  $scope.searching = true;
                  Scatterplot.display($scope.graphId, $scope.filter, $scope.size, $scope.range, $scope.facet, $scope.range2, $scope.facet2)
                      .success(function() {
                          $scope.searching = false;
                      })
                      .error(function() {
                          $scope.searching = false;
                      });
              };
          }
      });
    }).
    controller('HistoryDetailCtrl', function($scope, $routeParams, appConfig, Project, History) {
      $scope.projectId = $routeParams.projectId;
      $scope.queryProject = Project.query({projectId: $scope.projectId});

      // construct path to project history
      var config = appConfig.historyServer;
      var graphHistoryPath = config.storage + "/" + $scope.projectId;
      console.log(History.serverUrl() + "/#/repository?path="+encodeURIComponent(graphHistoryPath));

      $scope.historyUrl = History.serverUrl() + "/#/repository?path="+encodeURIComponent(graphHistoryPath);
    });
