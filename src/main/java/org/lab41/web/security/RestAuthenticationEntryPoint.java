package org.lab41.web.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Commences an authentication scheme.  Uponeon failure this will return a 401.
     * <p/>
     * <code>ExceptionTranslationFilter</code> will populate the <code>HttpSession</code> attribute named
     * <code>AbstractAuthenticationProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY</code> with the requested target URL before
     * calling this method.
     * <p/>
     * Implementations should modify the headers on the <code>ServletResponse</code> as necessary to
     * commence the authentication process.
     *
     * @param request       that resulted in an <code>AuthenticationException</code>
     * @param response      so that the user agent can begin authentication
     * @param authException that caused the invocation
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    }

}
