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

/* Directives */
angular.module('dendrite.directives', []).
  // Use: <span access-level='accessLevels.ROLE_USER'>test data</span>
  // Note: accessLevels must be set in controller
  directive('accessLevel', ['$rootScope', 'User', function($rootScope, User) {
    return {
        restrict: 'A',
        link: function($scope, element, attrs) {
            var prevDisp = element.css('display')
                , userRole
                , accessLevel;

            $scope.user = User.user;
            $scope.$watch('user', function(user) {
                if(user.role)
                    userRole = user.role;
                updateCSS();
            }, true);

            attrs.$observe('accessLevel', function(al) {
                if(al) accessLevel = $scope.$eval(al);
                updateCSS();
            });

            function updateCSS() {
                if(userRole && accessLevel) {
                    if(!User.authorize(accessLevel, userRole))
                        element.css('display', 'none');
                    else
                        element.css('display', prevDisp);
                }
            }
        }
    };
  }]).
  directive('fileParseGraph', ['$rootScope', 'appConfig', 'Helpers', function($rootScope, appConfig, Helpers) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {

          element.bind('change', function(evt) {
            if (!appConfig.fileUpload.parseGraphFile) {
              scope.$emit('event:graphFileParsed');
            }
            else {
              scope.$parent.selectedCheckboxesList = "";
              scope.$parent.selectedCheckboxes = [];

              // verify fileReader API support
              if (window.File && window.FileReader && window.FileList && window.Blob) {

                var reader = new FileReader();
                var f = evt.target.files[0];
                var format = document.getElementById('file-import-format').value;

                // capture the file information.
                reader.onload = function(e) {

                  var searchKeys = Helpers.parseGraphFile(reader.result, format);
                  scope.$parent.keysForGraph = searchKeys;
                  scope.$emit('event:graphFileParsed');
                }

                // read in the file
                var blob = f.slice(0, appConfig.fileUpload.maxBytesLocal);
                reader.readAsBinaryString(blob);
              } else {
                console.log('The File APIs are not fully supported in this browser.');
              }
            }
          });
        }
    };
  }]).
  directive('sigmajsGraph', ['$rootScope', '$q', '$compile', 'GraphTransform', function($rootScope, $q, $compile, GraphTransform) {
    return {
      restrict: 'A',
      transclude: true,
      link: function($scope, element, attrs) {

        // only generate visualization one time
        if ($scope.$parent.sigmaViz === undefined) {
          $scope.sigmaGraph = {nodes: [], edges: []};

          // build nodes into SigmaJS format
          var vertexSize = 20/$scope.sigmajsGraphData.vertices.length;
          var vertexColor = "#424a4a";
          $scope.sigmajsGraphData.vertices.forEach(function(vertex) {
            vertex.id = vertex._id;
            vertex.x = Math.random();
            vertex.y = Math.random();
            vertex.color = vertexColor;
            vertex.size = vertexSize;
            vertex.label = vertex.name;

            // push vertex
            $scope.sigmaGraph.nodes.push(vertex);
          });

          // build edges into SigmaJS format
          $scope.sigmajsGraphData.edges.forEach(function(edge) {
            edge.id = edge._id;
            edge.source = edge._inV;
            edge.target = edge._outV;

            // push edge
            $scope.sigmaGraph.edges.push(edge);
          });

          // generate sigma visualization
          // attach to parent scope to avoid future refreshes
          $scope.$parent.sigmaViz = new sigma({
            graph: $scope.sigmaGraph
          })
          .startForceAtlas2();
        }

        // render visualization, regardless of when displayed
        if ($scope.$parent.sigmaViz !== undefined) {
          var r = $scope.$parent.sigmaViz.addRenderer({
            type: 'canvas',
            container: document.getElementById('container-graph-sigmajs')
          });
          $scope.$parent.sigmaViz.refresh();

          // alert app to data completion
          $rootScope.$broadcast('event:reloadGraph');
        }
      }
    };
  }]).
  directive('tabs', ['$rootScope', function($rootScope) {
      return {
          restrict: 'A',
          link: function($scope, element, attrs) {
              var jqueryElement = $(element[0]);
              $scope.tabElement = jqueryElement;
          }
      };
  }]).
  directive('forceDirectedGraph', ['$rootScope', '$q', '$compile', function($rootScope, $q, $compile) {
    return {
      restrict: 'A',
      link: function($scope, element, attrs) {
        var width = $('#tabs').width()*0.90,
          height = 500;

        var color = d3.scale.category20();

        var force = d3.layout.force()
          .on("tick", tick)
          .charge(-120)
          .linkDistance(30)
          .size([width, height]);

        var svg = d3.select(element[0])
          .attr("width", width)
          .attr("height", height);

        var nodes = [], links = [];

        $scope.$watch(attrs.data, function(data) {
          if (data) {
            $q.all([
                data.vertices.promise,
                data.edges.promise
              ]).then(function(data) {
                var vertices = data[0];
                var edges = data[1];

                nodes = vertices.map(function(vertex) {
                  return {
                    _id: vertex._id,
                    name: vertex.name
                  };
                });

                var vertexToNode = {};
                vertices.forEach(function(vertex, idx) {
                  vertexToNode[vertex._id] = idx;
                })

                links = edges.map(function(edge) {
                  return {
                    _id: edge._id,
                    source: vertexToNode[edge._inV],
                    target: vertexToNode[edge._outV]
                  };
                });

                update();
              });
          } else {
            nodes = [];
            links = [];
            update();
          }
        });

        function update() {
          force
            .nodes(nodes)
            .links(links)
            .start();

          // Update the links.
          var link = svg.selectAll("line")
            .data(links, function(d) { return d._id; });

          // Enter any new links.
          link.enter().insert("svg:line")
            //.attr("class", "link")
            .style("stroke", "#999")
            .style("stroke-opacity", "0.6")
            .style("stroke-width", function(d) { return Math.sqrt(d.value); });

          // Exit any old links.
          link.exit().remove();

          // Update the nodes.
          var node = svg.selectAll("circle")
            .data(nodes, function(d) { return d._id; });

          // Enter any new nodes.
          node.enter().append("circle")
            //.attr("class", "node")
            .attr("r", 7)
            .attr("popover", function(d) {
                if (d.name !== undefined) {
                  return d.name;
                }
                else {
                  return d._id;
                }
             })
            .attr("popover-trigger", "mouseenter")
            .attr("popover-append-to-body", "true")
            .style("stroke", "#fff")
            .style("stroke-width", "1.5px")
            .style("fill", function(d) { return color(d._id); })
            .call(force.drag);

          // Exit any old nodes.
          node.exit().remove();

          node.append("title")
            .text(function(d) { return d.name; });


          // popover on svg elements requires recompile
          element.removeAttr("force-directed-graph");
          $compile(element)($scope);

          // alert app to data in the project
          if (nodes.length) {

          }
        }

        function tick() {
          // Update the links.
          var link = svg.selectAll("line")
            .data(links, function(d) { return d._id; });

          link.attr("x1", function(d) { return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });

          // Update the nodes.
          var node = svg.selectAll("circle")
            .data(nodes, function(d) { return d._id; });

          node.attr("cx", function(d) { return d.x; })
            .attr("cy", function(d) { return d.y; });
        }
      }
    };
  }]).
  directive('ngConfirmClick', [
    function(){
      return {
        priority: 1,
        restrict: 'A',
        link: function(scope, element, attrs){
          element.bind('click', function(e){
            var message = attrs.ngConfirmClick;
            if(message && !confirm(message)){
              e.stopImmediatePropagation();
              e.preventDefault();
            }
          });
        }
      }
    }
  ])
  .directive('autoFocus', function() {
     return function(scope, elem, attr) {
        elem[0].focus();
     };
  }).
  // <tabset>
  //    <tab ..>
  //    <tab ..>
  // </tabset>
  // tabset enables lazy loading of tab content to avoid unnecessary overhead, as well as
  // force refresh that AngularJS might otherwise not apply to DOM
  directive('tabset', function () {
    return {
      restrict: 'E',
      replace: true,
      transclude: true,
      controller: function($scope) {
        $scope.templateUrl = '';
        var tabs = $scope.tabs = [];
        var controller = this;

        this.selectTab = function (tab) {
          angular.forEach(tabs, function (tab) {
            tab.selected = false;
          });
          tab.selected = true;
        };

        this.setTabTemplate = function (templateUrl) {
          $scope.templateUrl = templateUrl;
        }

        this.addTab = function (tab) {
          if (tabs.length == 0) {
            controller.selectTab(tab);
          }
          tabs.push(tab);
        };
      },
      template:
        '<div class="row-fluid">' +
          '<div class="row-fluid">' +
            '<div class="nav nav-tabs" ng-transclude></div>' +
          '</div>' +
          '<div id="tabs-content" class="row-fluid">' +
            '<ng-include src="templateUrl">' +
          '</ng-include></div>' +
        '</div>'
    };
  }).
  directive('tab', function () {
    return {
      restrict: 'E',
      replace: true,
      require: '^tabset',
      scope: {
        title: '@',
        templateUrl: '@'
      },
      link: function(scope, element, attrs, tabsetController) {
        tabsetController.addTab(scope);

        scope.select = function () {
          tabsetController.selectTab(scope);
        }

        scope.$watch('selected', function () {
          if (scope.selected) {
            tabsetController.setTabTemplate(scope.templateUrl);
          }
        });
      },
      template:
        '<li ng-class="{active: selected}">' +
          '<a href="" ng-click="select()">{{ title }}</a>' +
        '</li>'
    };
  });
