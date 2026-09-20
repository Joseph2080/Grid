package org.bazar.vektrlabs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Server-side session auth via AWS Cognito (Authorization Code flow with a
 * confidential client, i.e. Spring Security's oauth2Login()). Catalogue
 * browsing (stores/categories/products/variants/locations) stays public.
 * AI chat, cart, checkout and store creation require an authenticated session
 * and complete profile. Profile onboarding requires authentication only. Services repeat the profile and
 * ownership checks for non-HTTP entry points such as AI tools.
 * <p>
 * The AI chat endpoints and the Swagger/OpenAPI docs are further restricted
 * to users whose Cognito identity carries the "ADMIN" group (mapped to
 * authority ROLE_ADMIN by {@link CognitoOidcUserService}).
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final CognitoLogoutSuccessHandler cognitoLogoutSuccessHandler;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final CompleteProfileAuthorizationManager completeProfileAuthorizationManager;
    private final CognitoOidcUserService cognitoOidcUserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        var csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax"));
        http
                .csrf(csrf -> csrf
                        // Session-cookie auth needs CSRF protection. Token is
                        // readable by JS (httpOnly=false) so same-origin
                        // fetch() calls can send it back as X-XSRF-TOKEN.
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Jericho's browser callback trusts ?success, not Stripe's payment status.
                        // Keep it closed until the payment integration verifies provider state.
                        //.requestMatchers("/api/v1/payments/callback").denyAll()
                        .requestMatchers(
                                "/oauth2/authorization/cognito",
                                "/login/oauth2/code/cognito", "/login",
                                "/css/**", "/js/**", "/images/**", "/assets/**", "/webjars/**", "/favicon.ico")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/complete-profile", "/auth/continue", "/api/csrf",
                                "/{slug:[^\\.]+}",
                                "/{slug:[^\\.]+}/product/**",
                                "/api/v1/store/**",
                                "/api/v1/category/**",
                                "/api/v1/products/**",
                                "/api/v1/product-variants/**",
                                "/api/v1/product-media-resources/**",
                                "/api/v1/location/**",
                                "/api/v1/attribute-schemas/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ai/chat", "/api/v1/chat")
                        .access(AuthorizationManagers.allOf(
                                completeProfileAuthorizationManager,
                                AuthorityAuthorizationManager.hasRole("ADMIN")))
                        .requestMatchers("/api/v1/payments/callback").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/user-profiles/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/user-profiles").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/user-profiles/me").authenticated()
                        .anyRequest().access(completeProfileAuthorizationManager))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(cognitoOidcUserService))
                        .defaultSuccessUrl("/auth/continue", true)
                        .failureUrl("/auth/continue?error=login"))
                .logout(logout -> logout
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(cognitoLogoutSuccessHandler));

        return http.build();
    }

}
