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
