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

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Custom Authentication entry point which returns a 401 upon failure.
 *
 * @author kramachandran
 */
public class RestAuthenticationEntryPointTest {
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Before
    public void setUp() throws Exception {
        restAuthenticationEntryPoint = new RestAuthenticationEntryPoint();

    }

    @Test
    public void testCommence() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new BadCredentialsException("Bad Credential");

        restAuthenticationEntryPoint.commence(request, response, exception);

        assertEquals(response.getStatus(), MockHttpServletResponse.SC_UNAUTHORIZED);

    }

}
