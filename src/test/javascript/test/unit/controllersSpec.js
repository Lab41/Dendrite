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


  describe('EdgeListCtrl', function(){
    var $httpBackend, scope, ctrl;

    beforeEach(inject(function($rootScope, _$httpBackend_, $controller){
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('rexster-resource/graphs/a/edges').respond({
        version: "2.3.0",
        results:[{
            _id: "Q1X-8-2F0LaTPQAK",
            _type: "edge",
            _outV: 8,
            _inV: 4,
            _label: "a"
        }],
        totalSize: 1,
        queryTime: 7.591168
      });

      scope = $rootScope.$new();
      ctrl = $controller('EdgeListCtrl', {
          $scope: scope,
          $routeParams: {graphId: "a"}
      });
    }));

    afterEach(function(){
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });


    it('should create "edge" model with 2 edges fetched from xhr', function(){
      expect(scope.query).toEqualData({});

      $httpBackend.flush();

      expect(scope.query).toEqualData({
        version: "2.3.0",
        results: [{
            _id: "Q1X-8-2F0LaTPQAK",
            _type: "edge",
            _outV: 8,
            _inV: 4,
            _label: "a"
        }],
        totalSize: 1,
        queryTime: 7.591168
      });
    });
  });


  describe('EdgeDetailCtrl', function(){
    var $httpBackend, scope, ctrl;

    beforeEach(inject(function($rootScope, _$httpBackend_, $controller){
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('rexster-resource/graphs/a/edges/Q1X-8-2F0LaTPQAK').respond({
        version: "2.3.0",
        results: [{
            _id: "Q1X-8-2F0LaTPQAK",
            _type: "edge",
            _outV: 8,
            _inV: 4,
            _label: "a"
        }],
        totalSize: 1,
        queryTime: 2.238976
      });

      scope = $rootScope.$new();
      ctrl = $controller('EdgeDetailCtrl', {
          $scope: scope,
          $routeParams: {graphId: "a", edgeId: "Q1X-8-2F0LaTPQAK"}
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
        version: "2.3.0",
        results: [{
            _id: "Q1X-8-2F0LaTPQAK",
            _type: "edge",
            _outV: 8,
            _inV: 4,
            _label: "a"
        }],
        totalSize: 1,
        queryTime: 2.238976
      });
    });
  });
});
