package org.bazar.vektrlabs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bazar.vektrlabs.config.CognitoLogoutSuccessHandler;
import org.bazar.vektrlabs.config.CognitoOidcUserService;
import org.bazar.vektrlabs.config.CompleteProfileAuthorizationManager;
import org.bazar.vektrlabs.config.RestAccessDeniedHandler;
import org.bazar.vektrlabs.config.RestAuthenticationEntryPoint;
import org.bazar.vektrlabs.config.SecurityConfig;
import org.bazar.vektrlabs.controller.rest.SessionController;
import org.bazar.vektrlabs.exception.GlobalExceptionHandler;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityConfigTest.TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "vektrlabs.cognito.logout-uri=https://idp.example/logout",
        "vektrlabs.cognito.client-id=test-client",
        "vektrlabs.cognito.post-logout-redirect-uri=https://shop.example/"
})
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private CurrentUserUtil currentUser;
    @Autowired
    private UserProfileService profiles;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(currentUser, profiles);
        when(currentUser.currentCognitoSub()).thenReturn("trusted-sub");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void actualCatalogueGetsArePublicButMutationsAndUnknownApisAreNot() throws Exception {
        for (String path : new String[]{
                "/api/v1/store/demo", "/api/v1/category", "/api/v1/products",
                "/api/v1/product-variants", "/api/v1/product-media-resources",
                "/api/v1/location/by-store/demo", "/api/v1/attribute-schemas"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
            mvc.perform(post(path).with(csrf())).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors.code").value("AUTHENTICATION_REQUIRED"));
        }
        mvc.perform(get("/api/private")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/product/demo")).andExpect(status().isUnauthorized());
        verify(profiles, never()).requireCompleteProfileByCognitoSub(anyString());
    }

    @Test
    void shareableProductDeepLinkIsPublic() throws Exception {
        mvc.perform(get("/demo-store/product/demo-product")).andExpect(status().isOk());
        verify(profiles, never()).requireCompleteProfileByCognitoSub(anyString());
    }

    @Test
    void omegaChatRequiresAuthenticationAdminRoleAndCsrf() throws Exception {
        var admin = oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        for (String path : new String[]{"/api/v1/ai/chat", "/api/v1/chat"}) {
            mvc.perform(post(path)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors.code").value("CSRF_INVALID"));
            mvc.perform(post(path).with(csrf())).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors.code").value("AUTHENTICATION_REQUIRED"));
            mvc.perform(post(path).with(admin)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors.code").value("CSRF_INVALID"));
            mvc.perform(post(path).with(admin).with(csrf().useInvalidToken())).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors.code").value("CSRF_INVALID"));
            mvc.perform(post(path).with(oidcLogin()).with(csrf())).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors.code").value("ACCESS_DENIED"));
            mvc.perform(post(path).with(admin).with(csrf())).andExpect(status().isOk());
        }
        verify(profiles, times(4)).requireCompleteProfileByCognitoSub("trusted-sub");
    }

    @Test
    void swaggerAndOpenApiDocsRequireAdminRole() throws Exception {
        for (String path : new String[]{"/v3/api-docs", "/swagger-ui/index.html"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errors.code").value("AUTHENTICATION_REQUIRED"));
            mvc.perform(get(path).with(oidcLogin())).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors.code").value("ACCESS_DENIED"));
            mvc.perform(get(path).with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void bootstrapCookieAndHeaderWorkWithoutDisablingCsrf() throws Exception {
        var bootstrap = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(cookie().exists("XSRF-TOKEN")).andReturn().getResponse();
        var token = new ObjectMapper().readTree(bootstrap.getContentAsString()).path("data");
        var cookie = bootstrap.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        var admin = oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mvc.perform(post("/api/v1/ai/chat").with(admin).cookie(cookie)
                        .header(token.path("headerName").asText(), token.path("token").asText()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/ai/chat").with(admin).cookie(cookie).header("X-XSRF-TOKEN", "forged"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errors.code").value("CSRF_INVALID"));
    }

    @Test
    void apiAuthenticationDoesNotRedirectToHtml() throws Exception {
        mvc.perform(get("/api/v1/shopping/cart/cart-id"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.errors.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void sessionWithoutTrustedIdentityClaimsCannotReachBusinessEndpoints() throws Exception {
        when(currentUser.currentCognitoSub()).thenThrow(new InsufficientAuthenticationException("Missing sub"));
        mvc.perform(get("/api/private").with(oidcLogin())).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors.code").value("AUTHENTICATION_REQUIRED"));
        verify(profiles, never()).requireCompleteProfileByCognitoSub(anyString());
    }

    @Test
    void loginStartsAuthorizationCodeFlowAndKeepsARequestSession() throws Exception {
        var result = mvc.perform(get("/oauth2/authorization/cognito")).andExpect(status().isFound()).andReturn();
        String location = result.getResponse().getRedirectedUrl();
        assertNotNull(location);
        assertTrue(location.startsWith("https://idp.example/authorize?"));
        assertTrue(location.contains("response_type=code"));
        assertTrue(location.contains("scope=openid"));
        assertTrue(location.contains("state="));
        assertNotNull(result.getRequest().getSession(false));
    }

    @Test
    void missingAndIncompleteProfilesAreMappedInsideTheSecurityFilterChain() throws Exception {
        when(profiles.requireCompleteProfileByCognitoSub("trusted-sub"))
                .thenThrow(new ProfileNotFoundException("Missing"));
        mvc.perform(get("/api/v1/shopping/cart/cart-id").with(oidcLogin()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.errors.code").value("PROFILE_NOT_FOUND"));
        doThrow(new ProfileIncompleteException("Incomplete")).when(profiles)
                .requireCompleteProfileByCognitoSub("trusted-sub");
        mvc.perform(post("/api/v1/store/demo").with(oidcLogin()).with(csrf()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errors.code").value("PROFILE_INCOMPLETE"));
    }

    @Test
    void completeProfileCanReachProtectedEndpoints() throws Exception {
        mvc.perform(get("/api/v1/shopping/cart/cart-id").with(oidcLogin())).andExpect(status().isOk());
        mvc.perform(put("/api/private").with(oidcLogin()).with(csrf())).andExpect(status().isOk());
        mvc.perform(delete("/api/private").with(oidcLogin()).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/private").with(oidcLogin())).andExpect(status().isForbidden());
    }

    @Test
    void onboardingNeedsAuthenticationButNeverACompleteProfile() throws Exception {
        when(profiles.requireCompleteProfileByCognitoSub(anyString()))
                .thenThrow(new ProfileIncompleteException("Incomplete"));
        mvc.perform(get("/api/user-profiles/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/user-profiles/me").with(oidcLogin())).andExpect(status().isOk());
        mvc.perform(post("/api/user-profiles").with(oidcLogin()).with(csrf())).andExpect(status().isOk());
        mvc.perform(put("/api/user-profiles/me").with(oidcLogin()).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/complete-profile")).andExpect(status().isOk()).andExpect(view().name("complete-profile"));
        mvc.perform(get("/auth/continue")).andExpect(status().isOk()).andExpect(view().name("auth-continue"));
        verify(profiles, never()).requireCompleteProfileByCognitoSub(anyString());
    }

    @Test
    void mvcSecurityExceptionsKeepTheirSpecificCodes() throws Exception {
        mvc.perform(get("/api/test/missing").with(oidcLogin()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.errors.code").value("PROFILE_NOT_FOUND"));
        mvc.perform(get("/api/test/incomplete").with(oidcLogin()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errors.code").value("PROFILE_INCOMPLETE"));
        mvc.perform(get("/api/test/denied").with(oidcLogin()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errors.code").value("ACCESS_DENIED"));
    }

    @Test
    void unverifiedPaymentReturnCannotPublishAPaymentEvent() throws Exception {
        mvc.perform(get("/api/v1/payments/callback").param("success", "true").param("sessionId", "forged"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/payments/callback").with(oidcLogin())
                        .param("success", "true").param("sessionId", "forged"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRequiresCsrfClearsTokenAndUsesConfiguredDestinationNotHost() throws Exception {
        mvc.perform(post("/logout").with(oidcLogin())).andExpect(status().isForbidden());
        var bootstrap = mvc.perform(get("/api/csrf")).andReturn().getResponse();
        var token = new ObjectMapper().readTree(bootstrap.getContentAsString()).path("data").path("token").asText();
        mvc.perform(post("/logout").with(oidcLogin()).cookie(bootstrap.getCookie("XSRF-TOKEN"))
                        .header("X-XSRF-TOKEN", token).header("Host", "untrusted.example"))
                .andExpect(status().isFound())
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0))
                .andExpect(redirectedUrl("https://idp.example/logout?client_id=test-client&logout_uri=https://shop.example/"));
        var fresh = mvc.perform(get("/api/csrf")).andReturn().getResponse();
        assertNotEquals(token,
                new ObjectMapper().readTree(fresh.getContentAsString()).path("data").path("token").asText());
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
            CompleteProfileAuthorizationManager.class, CognitoLogoutSuccessHandler.class,
            CognitoOidcUserService.class,
            SessionController.class, GlobalExceptionHandler.class, TestEndpoints.class})
    static class TestConfig {
        @Bean
        CurrentUserUtil currentUser() {
            return mock(CurrentUserUtil.class);
        }

        @Bean
        UserProfileService profiles() {
            return mock(UserProfileService.class);
        }

        @Bean
        ClientRegistrationRepository clients() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("cognito")
                    .clientId("test-client").clientSecret("test-only")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("openid", "email")
                    .authorizationUri("https://idp.example/authorize").tokenUri("https://idp.example/token")
                    .jwkSetUri("https://idp.example/jwks").userInfoUri("https://idp.example/user")
                    .userNameAttributeName("sub").clientName("Cognito").build());
        }

        @Bean
        InternalResourceViewResolver views() {
            return new InternalResourceViewResolver("/templates/", ".html");
        }
    }

    @RestController
    static class TestEndpoints {
        @RequestMapping({"/api/v1/store/demo", "/api/v1/category", "/api/v1/products",
                "/api/v1/product-variants", "/api/v1/product-media-resources", "/api/v1/attribute-schemas",
                "/api/v1/location/by-store/demo", "/api/v1/ai/chat", "/api/v1/chat",
                "/api/v1/shopping/cart/cart-id", "/api/private", "/api/user-profiles",
                "/api/user-profiles/me", "/api/v1/payments/callback", "/demo-store/product/demo-product",
                "/v3/api-docs", "/swagger-ui/index.html"})
        Map<String, Object> ok() {
            return Map.of("data", "ok");
        }

        @GetMapping("/api/test/missing")
        void missing() {
            throw new ProfileNotFoundException("Missing");
        }

        @GetMapping("/api/test/incomplete")
        void incomplete() {
            throw new ProfileIncompleteException("Incomplete");
        }

        @GetMapping("/api/test/denied")
        void denied() {
            throw new AccessDeniedException("Someone else's resource");
        }
    }
}
