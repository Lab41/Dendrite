package org.lab41.web.security;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class will overwrite the default AuthenticationSuccessHandler to return a Jackson object rather than to redirect.
 * This is particularly useful when working on single screen applications.
 *
 * @author kramachandran
 */
public class JsonAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    //TODO: Change this to use Spring JacksonObjectMapperFactory?
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        objectMapper.writeValue(response.getOutputStream(), authentication);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
