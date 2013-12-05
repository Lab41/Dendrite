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

package org.lab41.dendrite.web.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    final private String X_REQUESTED_WITH = "x-requested-with";
    final private String XML_HTTP_REQUEST = "XMLHttpRequest";

    /**
     * Commences an authentication scheme.  Uponeon failure this will return a 401.
     * <p/>
     * <code>ExceptionTranslationFilter</code> will populate the <code>HttpSession</code> attribute named
     * <code>AbstractAuthenticationProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY</code> with the requested target URL before
     * calling this method.
     * <p/>
     * Implementations should modify the headers on the <code>ServletResponse</code> as necessary to
     * commence the authentication process.
     *
     * @param request       that resulted in an <code>AuthenticationException</code>
     * @param response      so that the user agent can begin authentication
     * @param authException that caused the invocation
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        if (!response.isCommitted()) {
            String ajax = request.getHeader(X_REQUESTED_WITH);

            // Don't include the `WWW-Authenticate` header for Ajax calls
            if (!XML_HTTP_REQUEST.equals(ajax)) {
                response.addHeader("WWW-Authenticate", "Basic realm=\"\"");
            }
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
    }

}
