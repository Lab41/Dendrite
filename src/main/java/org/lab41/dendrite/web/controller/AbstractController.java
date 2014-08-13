/*
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

package org.lab41.dendrite.web.controller;

import org.lab41.dendrite.metagraph.CannotDeleteCurrentBranchException;
import org.lab41.dendrite.metagraph.MetaGraphTx;
import org.lab41.dendrite.metagraph.NotFound;
import org.lab41.dendrite.services.MetaGraphService;
import org.lab41.dendrite.web.responses.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public abstract class AbstractController {

    @Autowired
    protected MetaGraphService metaGraphService;

    @Autowired
    TaskExecutor taskExecutor;

    class BindingException extends Exception {
        public BindingException(BindingResult result) {
            super(result.toString());
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFound.class)
    public ErrorResponse handleNotFound(NotFound notFound) {
        return new ErrorResponse(notFound.getLocalizedMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindingException.class)
    public ErrorResponse handleBindingException(BindingException bindingException) {
        return new ErrorResponse(bindingException.getLocalizedMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(CannotDeleteCurrentBranchException.class)
    public ErrorResponse handleCannotDeleteCurrentBranch() {
        return new ErrorResponse("cannot delete current branch");
    }
}
