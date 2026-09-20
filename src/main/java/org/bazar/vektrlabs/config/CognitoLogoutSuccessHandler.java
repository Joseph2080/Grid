package org.bazar.vektrlabs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * After clearing the local session, also ends the Cognito Hosted UI session
 * so a subsequent login doesn't silently re-authenticate the same user.
 */
@Component
public class CognitoLogoutSuccessHandler implements LogoutSuccessHandler {

    @Value("${vektrlabs.cognito.logout-uri}")
    private String cognitoLogoutUri;

    @Value("${vektrlabs.cognito.client-id}")
    private String cognitoClientId;

    @Value("${vektrlabs.cognito.post-logout-redirect-uri}")
    private String postLogoutRedirectUri;

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        String logoutUrl = UriComponentsBuilder
                .fromUriString(cognitoLogoutUri)
                .queryParam("client_id", cognitoClientId)
                .queryParam("logout_uri", postLogoutRedirectUri)
                .build().encode()
                .toUriString();

        response.sendRedirect(logoutUrl);
    }
}
