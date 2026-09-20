package org.bazar.vektrlabs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a plain JSON 401 for unauthenticated API requests instead of
 * Spring Security's default behaviour of redirecting to the OAuth2
 * authorization endpoint. The frontend triggers login explicitly (via a
 * top-level navigation to /oauth2/authorization/cognito), so same-origin
 * fetch() calls should never be silently redirected.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        SecurityErrorResponses.write(response, HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "Authentication required.");
    }
}
