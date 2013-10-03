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

package org.lab41.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Used by the {@link org.springframework.security.web.access.ExceptionTranslationFilter} to commence a form login
 * authentication via the {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}.
 * <p/>
 * Upon authentication failure it simply returns a response code of 401.
 * <p/>
 * TODO: add a force HTTPS feature.
 * <p/>
 * <p/>
 * <p/>
 * <p/>
 * This Authentication Entry point c
 * Created with IntelliJ IDEA.
 * User: kramachandran-admin
 * Date: 7/24/13
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
