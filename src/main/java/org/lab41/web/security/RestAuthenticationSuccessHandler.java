package org.lab41.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
public class RestAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
