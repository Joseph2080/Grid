package org.bazar.vektrlabs.directprofile;

import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserUtilTest {
    private final CurrentUserUtil currentUser = new CurrentUserUtil();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsSubjectClaimRatherThanOAuthUsername() {
        Fixtures.signIn("validated-subject");

        assertEquals("not-the-subject", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals("validated-subject", currentUser.currentCognitoSub());
        assertEquals("validated-subject@example.test", currentUser.currentEmailClaim());
    }

    @Test
    void missingAuthenticationFailsClosed() {
        SecurityContextHolder.clearContext();

        assertThrows(InsufficientAuthenticationException.class, currentUser::currentCognitoSub);
        assertThrows(InsufficientAuthenticationException.class, currentUser::currentEmailClaim);
    }

    @Test
    void anonymousAuthenticationFailsClosed() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThrows(InsufficientAuthenticationException.class, currentUser::currentCognitoSub);
    }

    @Test
    void unauthenticatedOAuthPrincipalFailsClosed() {
        var principal = Fixtures.authentication("buyer").getPrincipal();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated(principal, null));

        assertThrows(InsufficientAuthenticationException.class, currentUser::currentCognitoSub);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "", " \t"})
    void missingOrBlankSubjectCannotFallBackToUsername(String subject) {
        var attributes = new HashMap<String, Object>(Map.of("username", "looks-authenticated"));
        if (!subject.equals("missing")) {
            attributes.put("sub", subject);
        }
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var user = new DefaultOAuth2User(authorities, attributes, "username");
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(user, authorities, "cognito"));

        assertThrows(InsufficientAuthenticationException.class, currentUser::currentCognitoSub);
    }
}
