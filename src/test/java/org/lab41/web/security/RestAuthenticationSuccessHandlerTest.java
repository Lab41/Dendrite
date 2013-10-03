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

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author kramachandran
 */
public class RestAuthenticationSuccessHandlerTest {
    private RestAuthenticationSuccessHandler restAuthenticationSuccessHandler;
    private ObjectMapper mapper = new ObjectMapper();


    @After
    public void tearDown() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        restAuthenticationSuccessHandler = new RestAuthenticationSuccessHandler();
    }

    @Test
    public void testOnAuthenticationSuccess() throws IOException, ServletException {
        List<GrantedAuthority> authorityList = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER, ROLE_ADMIN");
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", authorityList);

        //Mock Objects
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        restAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(response.getStatus(), HttpServletResponse.SC_NO_CONTENT);
        assertEquals(response.getContentAsString(), "");
    }

}
