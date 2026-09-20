package org.bazar.vektrlabs.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoOidcUserServiceTest {

    private final CognitoOidcUserService service = new CognitoOidcUserService();

    @Test
    void mapsCognitoGroupsClaimToRoleAuthorities() {
        var idToken = idToken(Map.of("cognito:groups", List.of("ADMIN", "editors")));

        var user = service.loadUser(new OidcUserRequest(clientRegistration(), accessToken(), idToken));

        assertThat(user.getAuthorities())
                .extracting(a -> a.getAuthority())
                .contains("ROLE_ADMIN", "ROLE_EDITORS");
    }

    @Test
    void normalizesGroupNamesAndAvoidsDoublePrefixing() {
        var idToken = idToken(Map.of("cognito:groups",
                List.of("store-owners", "Content Managers", "ROLE_ADMIN", " ", "")));

        var user = service.loadUser(new OidcUserRequest(clientRegistration(), accessToken(), idToken));

        assertThat(user.getAuthorities())
                .extracting(a -> a.getAuthority())
                .contains("ROLE_STORE_OWNERS", "ROLE_CONTENT_MANAGERS", "ROLE_ADMIN")
                .doesNotContain("ROLE_ROLE_ADMIN", "ROLE_", "ROLE_ ");
    }

    @Test
    void usersWithoutGroupsClaimGetNoExtraRole() {
        var idToken = idToken(Map.of());

        var user = service.loadUser(new OidcUserRequest(clientRegistration(), accessToken(), idToken));

        assertThat(user.getAuthorities())
                .extracting(a -> a.getAuthority())
                .noneMatch(authority -> authority.startsWith("ROLE_"));
    }

    private OidcIdToken idToken(Map<String, Object> extraClaims) {
        var claims = new HashMap<>(extraClaims);
        claims.putIfAbsent(IdTokenClaimNames.SUB, "user-sub");
        return new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(60), claims);
    }

    private OAuth2AccessToken accessToken() {
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", Instant.now(),
                Instant.now().plusSeconds(60));
    }

    private ClientRegistration clientRegistration() {
        // No userinfo-endpoint configured, so OidcUserService derives the
        // user solely from ID token claims and never makes an HTTP call.
        return ClientRegistration.withRegistrationId("cognito")
                .clientId("test-client").clientSecret("test-only")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email")
                .authorizationUri("https://idp.example/authorize")
                .tokenUri("https://idp.example/token")
                .jwkSetUri("https://idp.example/jwks")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Cognito")
                .build();
    }
}
