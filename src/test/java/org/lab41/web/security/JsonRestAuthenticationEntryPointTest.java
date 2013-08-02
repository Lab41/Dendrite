package org.lab41.web.security;

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
public class JsonRestAuthenticationEntryPointTest {
    private JsonRestAuthenticationEntryPoint jsonRestAuthenticationEntryPoint;

    @Before
    public void setUp() throws Exception {
        jsonRestAuthenticationEntryPoint = new JsonRestAuthenticationEntryPoint();

    }

    @Test
    public void testCommence() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new BadCredentialsException("Bad Credential");

        jsonRestAuthenticationEntryPoint.commence(request, response, exception);

        assertEquals(response.getStatus(), MockHttpServletResponse.SC_UNAUTHORIZED);

    }

}
