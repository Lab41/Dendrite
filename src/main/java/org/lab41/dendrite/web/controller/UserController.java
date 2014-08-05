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

package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.models.ProjectMetadata;
import org.lab41.dendrite.metagraph.models.UserMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Controller("/api")
public class UserController {

    @Autowired
    MetaGraphService metaGraphService;

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public @ResponseBody Map<String,Object> userInformation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("name", authentication.getName());
        response.put("authorities", authentication.getAuthorities());

        return response;
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> getUsers() {
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        Map<String, Object> response = new HashMap<>();
        ArrayList<Object> projects = new ArrayList<>();
        response.put("users", projects);

        for(UserMetadata userMetadata: tx.getUsers()) {
            projects.add(getUserMap(userMetadata));
        }

        // Commit must come after all graph access.
        tx.commit();

        return response;
    }

    @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String userId) {

        Map<String, Object> response = new HashMap<>();
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();
        UserMetadata userMetadata = tx.getUser(userId);

        if (userMetadata == null) {
            response.put("status", "error");
            response.put("msg", "could not find user '" + userId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.put("user", getUserMap(userMetadata));

        // Commit must come after all graph access.
        tx.commit();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Map<String, Object> getUserMap(UserMetadata userMetadata) {
        Map<String, Object> user = new HashMap<>();

        user.put("_id", userMetadata.getId());
        user.put("name", userMetadata.getName());

        return user;
    }

}
