package org.bazar.vektrlabs.directprofile;

import org.bazar.vektrlabs.entity.UserProfile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class Fixtures {
    private Fixtures() {
    }

    static UserProfile profile(String sub) {
        var profile = new UserProfile();
        profile.setId(UUID.randomUUID());
        profile.setCognitoSub(sub);
        profile.setEmail(sub + "@example.test");
        profile.setFirstName("First");
        profile.setLastName("Last");
        return profile;
    }

    static Authentication authentication(String sub) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var principal = new DefaultOAuth2User(authorities,
                Map.of("sub", sub, "username", "not-the-subject", "email", sub + "@example.test"),
                "username");
        return new OAuth2AuthenticationToken(principal, authorities, "cognito");
    }

    static void signIn(String sub) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication(sub));
        SecurityContextHolder.setContext(context);
    }
}
