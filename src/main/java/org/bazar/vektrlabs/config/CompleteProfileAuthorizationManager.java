package org.bazar.vektrlabs.config;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class CompleteProfileAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final CurrentUserUtil currentUserUtil;
    private final UserProfileService userProfileService;

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication,
                                           RequestAuthorizationContext context) {
        if (!new AuthenticationTrustResolverImpl().isAuthenticated(authentication.get())) {
            return new AuthorizationDecision(false);
        }
        try {
            userProfileService.requireCompleteProfileByCognitoSub(currentUserUtil.currentCognitoSub());
            return new AuthorizationDecision(true);
        } catch (ProfileNotFoundException exception) {
            // ExceptionTranslationFilter handles access-denied exceptions, not domain 404s.
            throw new AccessDeniedException("Profile required.", exception);
        }
    }
}
