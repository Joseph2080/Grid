package org.bazar.vektrlabs.util;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CurrentUserUtil {

    public String currentCognitoSub() {
        String sub = currentOAuth2User().getAttribute("sub");
        if (!StringUtils.hasText(sub)) {
            throw new InsufficientAuthenticationException("The authenticated identity has no subject.");
        }
        return sub;
    }

    public String currentEmailClaim() {
        return currentOAuth2User().getAttribute("email");
    }

    private OAuth2User currentOAuth2User() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            throw new InsufficientAuthenticationException(
                    "A signed-in user is required for this action.");
        }
        return oAuth2User;
    }
}
