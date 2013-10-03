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

/* http://docs.angularjs.org/guide/dev_guide.e2e-testing */

describe('dendrite', function($window) {

  beforeEach(function() {
    browser().navigateTo('/');
  });

  describe('loginSuccess', function($window) {

    beforeEach(function() {
      browser().navigateTo('#/login/');
    });

    it('unauthenticated users should redirect to /login', function() {
      browser().navigateTo('#/graphs/');
      expect(browser().location().url()).toBe('/login');
      //pause();
    });

    it('login screen should display Login button', function() {
      expect(element('#form-login [ng-click]').text()).toMatch(/Login/);
      //pause();
    });

    it('should accept valid login credentials', function() {
      pause();
      input('username').enter('user');
      input('password').enter('password');
      pause();
      element('#form-login [ng-click]').click();
      sleep(1);
      expect(element('.button-logout').text()).toMatch(/Logout/);
      pause();
    });

    it('should logout after logging in with valid credentials', function() {
      browser().navigateTo('#/login/');
      input('username').enter('user');
      input('password').enter('password');
      element('#form-login [ng-click]').click();
      sleep(2);
      pause();
      expect(element('.button-logout').text()).toMatch(/Logout/);
      element('[ng-click]').click();
      console.log(element('[ng-click]'));
      pause();
      expect(element('#form-login [ng-click]').text()).toMatch(/Login/);
      //pause();
    });


  });

  describe('loginFailure', function($window) {
    it('should not accept invalid login credentials', function($window) {
      var alertSpy = jasmine.createSpy('alert');

      input('username').enter('fake');
      input('password').enter('password');
      //pause();
      element('#form-login [ng-click]').click();
      sleep(1);
      expect(element('#form-login [ng-click]').text()).toMatch(/Login/);
    });
  });
});
