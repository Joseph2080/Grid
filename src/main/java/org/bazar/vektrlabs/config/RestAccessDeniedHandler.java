package org.bazar.vektrlabs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        if (exception.getCause() instanceof ProfileNotFoundException) {
            SecurityErrorResponses.write(response, HttpStatus.NOT_FOUND,
                    "PROFILE_NOT_FOUND", "Complete your profile to continue.");
        } else if (exception instanceof ProfileIncompleteException) {
            SecurityErrorResponses.write(response, HttpStatus.FORBIDDEN,
                    "PROFILE_INCOMPLETE", "Complete your first and last name to continue.");
        } else if (exception instanceof CsrfException) {
            SecurityErrorResponses.write(response, HttpStatus.FORBIDDEN,
                    "CSRF_INVALID", "Refresh the page and try again.");
        } else {
            SecurityErrorResponses.write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied.");
        }
    }
}
