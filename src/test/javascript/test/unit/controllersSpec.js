'use strict';

/* jasmine specs for controllers go here */

describe('controllers', function(){
  beforeEach(function(){
    this.addMatchers({
      toEqualData: function(expected) {
        return angular.equals(this.actual, expected);
      }
    })
  });

  beforeEach(module('dendrite.controllers'));
  beforeEach(module('dendrite.services'));


  describe('GraphListCtrl', function(){
    var $httpBackend, scope, ctrl;

    beforeEach(inject(function($rootScope, _$httpBackend_, $controller){
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('rexster-resource/graphs').respond({
        graphs: ["a", "b"]
      });

      scope = $rootScope.$new();
      ctrl = $controller('GraphListCtrl', {$scope: scope});
    }));

    afterEach(function(){
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });


    it('should create "graphs" model with 2 graphs fetched from xhr', function(){
      expect(scope.query).toEqualData({});

      $httpBackend.flush();

      expect(scope.query).toEqualData({
        graphs: ["a", "b"]
      });
    });
  });


  describe('GraphDetailCtrl', function(){
    var $httpBackend, scope, ctrl;

    beforeEach(inject(function($rootScope, _$httpBackend_, $controller){
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('rexster-resource/graphs/a').respond({
        name: "a",
        queryTime: 0.1,
        readOnly: false,
        type: "titan",
        version: "0.3.1"
      });

      scope = $rootScope.$new();
      ctrl = $controller('GraphDetailCtrl', {
          $scope: scope,
          $routeParams: {graphId: "a"}
      });
    }));

    afterEach(function(){
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });


    it('should fetch graph detail', function(){
      expect(scope.graph).toEqualData({});

      $httpBackend.flush();

      expect(scope.graph).toEqualData({
        name: "a",
        queryTime: 0.1,
        readOnly: false,
        type: "titan",
        version: "0.3.1"
      });
    });
  });


  describe('VertexListCtrl', function(){
    var $httpBackend, scope, ctrl;

    beforeEach(inject(function($rootScope, _$httpBackend_, $controller){
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('rexster-resource/graphs/a/vertices').respond({
        results: [
            {_id: 4, _type: "vertex"},
            {_id: 8, _type: "vertex"}
        ],
        totalSize: 2
      });

      scope = $rootScope.$new();
      ctrl = $controller('VertexListCtrl', {
          $scope: scope,
          $routeParams: {graphId: "a"}
      });
    }));

    afterEach(function(){
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });


    it('should create "vertex" model with 2 vertices fetched from xhr', function(){
      expect(scope.query).toEqualData({});

      $httpBackend.flush();

      expect(scope.query).toEqualData({
        results: [
            {_id: 4, _type: "vertex"},
            {_id: 8, _type: "vertex"}
        ],
        totalSize: 2
      });
    });
  });


  describe('VertexDetailCtrl', function(){
    var $httpBackend, scope, ctrl;

    beforeEach(inject(function($rootScope, _$httpBackend_, $controller){
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('rexster-resource/graphs/a/vertices/4').respond({
        results: {_id: 4, _type: "vertex"}
      });

      scope = $rootScope.$new();
      ctrl = $controller('VertexDetailCtrl', {
          $scope: scope,
          $routeParams: {graphId: "a", vertexId: 4}
      });
    }));

    afterEach(function(){
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });


    it('should fetch graph detail', function(){
      expect(scope.query).toEqualData({});

      $httpBackend.flush();

      expect(scope.query).toEqualData({
        results: {_id: 4, _type: "vertex"}
      });
    });
  });
});
