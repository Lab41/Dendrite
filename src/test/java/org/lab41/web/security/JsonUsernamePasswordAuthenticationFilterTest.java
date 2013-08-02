package org.lab41.web.security;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * @author kramachandran
 */
public class JsonUsernamePasswordAuthenticationFilterTest {
    private JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter;

    @Before
    public void setUp() throws Exception {
        jsonUsernamePasswordAuthenticationFilter = new JsonUsernamePasswordAuthenticationFilter();

    }


    @Test
    public void testParseHttpRequest() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        MockHttpServletRequest mockbadHttpServletRequest = new MockHttpServletRequest();
        Map<String, String> objectAsMap = new HashMap<String, String>();
        objectAsMap.put(JsonUsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY, "user");
        objectAsMap.put(JsonUsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_PASSWORD_KEY, "password");

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonObject = objectMapper.writeValueAsString(objectAsMap);

        mockHttpServletRequest.setContent(jsonObject.getBytes());
        mockbadHttpServletRequest.setContent("".getBytes());

        Map response = jsonUsernamePasswordAuthenticationFilter.parseHttpRequest(mockHttpServletRequest);

        assertEquals(response.get(JsonUsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY), "user");

        Map nullResponse = jsonUsernamePasswordAuthenticationFilter.parseHttpRequest(mockbadHttpServletRequest);
        assertNull(nullResponse);


    }

    @Test
    public void testAttemptAuthentication() throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        mockHttpServletRequest.setMethod("POST");
        Map<String, String> objectAsMap = new HashMap<String, String>();
        objectAsMap.put(JsonUsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY, "user");
        objectAsMap.put(JsonUsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_PASSWORD_KEY, "password");

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonObject = objectMapper.writeValueAsString(objectAsMap);

        mockHttpServletRequest.setContent(jsonObject.getBytes());

        AuthenticationManager mockManager = mock(AuthenticationManager.class);

        when(mockManager.authenticate(isA(UsernamePasswordAuthenticationToken.class))).thenReturn(new UsernamePasswordAuthenticationToken("user", "password"));

        jsonUsernamePasswordAuthenticationFilter.setAuthenticationManager(mockManager);
        Authentication authentication = jsonUsernamePasswordAuthenticationFilter.attemptAuthentication(mockHttpServletRequest, mockHttpServletResponse);
        assertEquals(authentication.getName(), "user");

    }
}
