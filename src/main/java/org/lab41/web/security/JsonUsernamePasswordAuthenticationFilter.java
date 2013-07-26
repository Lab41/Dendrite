package org.lab41.web.security;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * This class is supposed to take a jason object with the properties j_username and j_password and use those
 * properties to authenticate against.
 *
 * @author kramachandran
 */
public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    //TODO: Change this to use Spring JacksonObjectMapperFactory?
    protected ObjectMapper mapper = new ObjectMapper();
    protected Logger logger = LoggerFactory.getLogger(JsonUsernamePasswordAuthenticationFilter.class);

    public Map parseHttpRequest(HttpServletRequest request) {
        Map retVal = null;
        try {
            retVal = (Map) mapper.readValue(request.getInputStream(), Object.class);
            logger.debug(retVal.toString());
            return retVal;
        } catch (Exception e) {
            logger.error("Error Parsing login object ", e);

        }
        return retVal;
    }

    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        String username = null;
        String password = null;
        //You have to do a POST
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        Map parsedRequest = parseHttpRequest(request);

        if (parsedRequest != null) {
            username = (String) parsedRequest.get(SPRING_SECURITY_FORM_USERNAME_KEY);
            password = (String) parsedRequest.get(SPRING_SECURITY_FORM_PASSWORD_KEY);
        }

        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = "";
        }

        username = username.trim();

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);

        // Allow subclasses to set the "details" property
        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
