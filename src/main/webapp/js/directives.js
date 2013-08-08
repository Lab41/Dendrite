'use strict';

/* Directives */
angular.module('dendrite.directives', [])
  // Use: <span access-level='accessLevels.ROLE_USER'>test data</span>
  // Note: accessLevels must be set in controller
  .directive('accessLevel', ['$rootScope', 'User', function($rootScope, User) {
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
  }]);
  
 
