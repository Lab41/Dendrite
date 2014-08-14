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

/* Filters */

angular.module('dendrite.filters', [])
  .filter('capitalize', function() {
    return function(input, scope) {
        return input.substring(0,1).toUpperCase()+input.substring(1);
    }
  })
  .filter('domainName', function() {
    return function(input, scope) {
        var el = document.createElement("a");
        el.href = input;
        return el.hostname;
    }
  })
  .filter('truncate', function() {
    return function(input, scope) {
      var retVal = input;
      var maxLen = 7;
      if (input !== undefined) {
        if (input.length > maxLen) {
          retVal = input.substring(0,maxLen)+"...";
        }
      }
      return retVal;
    }
  })
  // prettify from http://jsfiddle.net/KSTe8/
  .filter('prettify', function () {
    return function(json, scope) {
        json = JSON.stringify(json, undefined, 4);
        json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
            var cls = 'number';
            if (/^"/.test(match)) {
                if (/:$/.test(match)) {
                    cls = 'key';
                } else {
                    cls = 'string';
                }
            } else if (/true|false/.test(match)) {
                cls = 'boolean';
            } else if (/null/.test(match)) {
                cls = 'null';
            }
            match = match.replace(/"/g, '');
            return '<span class="' + cls + '">' + match + '</span>';
        });
    }
  });
