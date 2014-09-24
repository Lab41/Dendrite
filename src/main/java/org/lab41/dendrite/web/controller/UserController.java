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
import org.lab41.dendrite.metagraph.NotFound;
import org.lab41.dendrite.metagraph.models.UserMetadata;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.responses.GetUserResponse;
import org.lab41.dendrite.web.responses.GetUsersResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code UserController} is the main user interface for creating and manipulating users.
 */
@Controller
@RequestMapping("/api")
public class UserController extends AbstractController {

    @Autowired
    MetaGraphService metaGraphService;

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public @ResponseBody Map<String,Object> userInformation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("name", authentication.getName());
        response.put("authorities", authentication.getAuthorities());

        MetaGraphTx tx = metaGraphService.buildTransaction().start();

        // Make sure the user exists.
        try {
            tx.getOrCreateUser(authentication);
        } catch (Throwable t) {
            tx.rollback();
            throw t;
        }

        tx.commit();

        return response;
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    @ResponseBody
    public GetUsersResponse getUsers() {
        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            List<GetUserResponse> users = new ArrayList<>();

            for (UserMetadata userMetadata : tx.getUsers()) {
                users.add(new GetUserResponse(userMetadata));
            }

            return new GetUsersResponse(users);
        } finally {
            tx.commit();
        }
    }

    @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public GetUserResponse getUser(@PathVariable String userId) throws NotFound {

        MetaGraphTx tx = metaGraphService.buildTransaction().readOnly().start();

        try {
            UserMetadata userMetadata = tx.getUser(userId);

            if (userMetadata == null) {
                throw new NotFound(UserMetadata.class, userId);
            }

            return new GetUserResponse(userMetadata);
        } finally {
            tx.commit();
        }
    }
}
