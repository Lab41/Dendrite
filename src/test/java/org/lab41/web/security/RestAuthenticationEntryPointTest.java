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
