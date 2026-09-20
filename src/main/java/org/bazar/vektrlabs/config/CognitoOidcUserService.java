package org.bazar.vektrlabs.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Maps the Cognito "cognito:groups" ID-token claim onto Spring Security
 * authorities, e.g. group "ADMIN" becomes authority "ROLE_ADMIN". Admin
 * access is managed entirely in Cognito by adding/removing users from the
 * "ADMIN" group - no app redeploy needed to grant or revoke it. This lets
 * hasRole("ADMIN") checks in SecurityConfig gate Cognito-group-restricted
 * routes such as the Swagger/OpenAPI docs and the AI chat endpoints.
 */
@Component
public class CognitoOidcUserService extends OidcUserService {

    private static final String GROUPS_CLAIM = "cognito:groups";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(oidcUser.getAuthorities());
        List<String> groups = oidcUser.getIdToken().getClaimAsStringList(GROUPS_CLAIM);
        if (groups != null) {
            groups.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(group -> !group.isEmpty())
                    .map(this::toAuthority)
                    .forEach(authorities::add);
        }

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private GrantedAuthority toAuthority(String group) {
        String normalizedGroup = group
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        String authority = normalizedGroup.startsWith(ROLE_PREFIX)
                ? normalizedGroup
                : ROLE_PREFIX + normalizedGroup;
        return new SimpleGrantedAuthority(authority);
    }
}
