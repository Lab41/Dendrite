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
});
