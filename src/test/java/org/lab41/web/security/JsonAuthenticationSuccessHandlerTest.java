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
public class JsonAuthenticationSuccessHandlerTest {
    private JsonAuthenticationSuccessHandler jsonRestAuthenticationSuccessHandler;
    private ObjectMapper mapper = new ObjectMapper();


    @After
    public void tearDown() throws Exception {


    }

    @Before
    public void setUp() throws Exception {
        jsonRestAuthenticationSuccessHandler = new JsonAuthenticationSuccessHandler();

    }

    @Test
    public void testOnAuthenticationSuccess() throws IOException, ServletException {
        List<GrantedAuthority> authorityList = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER, ROLE_ADMIN");
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", authorityList);

        //Mock Objects
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jsonRestAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(response.getStatus(), HttpServletResponse.SC_OK);

        //Expected Response
        /*
            {"details":
                {"remoteAddress":"0:0:0:0:0:0:0:1",
                 "sessionId":"AEA0E8D38618341134B52FCE68CE5CE7"},
                 "authorities":[{"authority":"ROLE_USER"}],
                 "authenticated":true,
                 "principal":{"password":null,"username":"user","authorities":[{"authority":"ROLE_USER"}],
                 "accountNonExpired":true,
                 "accountNonLocked":true,
                 "credentialsNonExpired":true,"enabled":true},
                 "credentials":null,
                 "name":"user"}
         */
        Map object = (Map) mapper.readValue(response.getContentAsString(), Object.class);
        assertEquals(object.get("name"), "user");


    }

}
