package org.bazar.vektrlabs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal Swagger/OpenAPI setup, scoped to REST endpoints only (everything
 * under /api/**). Page/MVC controllers that render views (e.g.
 * StorePageController, ProductPageController) live outside /api and are
 * excluded from the generated docs by this path scoping alone - no
 * per-controller exclusion or per-endpoint annotations required.
 * <p>
 * Docs access itself is restricted to ROLE_ADMIN in SecurityConfig. Since
 * mutating endpoints also require a CSRF token, a global "X-XSRF-TOKEN"
 * apiKey security scheme is declared here so an admin can paste the token
 * (from GET /api/csrf) into Swagger UI's Authorize dialog once, and have
 * "Try it out" send it on every POST/PUT/DELETE call.
 */
@Configuration
public class OpenApiConfig {

    private static final String CSRF_SECURITY_SCHEME = "X-XSRF-TOKEN";

    @Bean
    public OpenAPI vektrlabsOpenApi() {
        return new OpenAPI().info(new Info()
                        .title("Vektrlabs API")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(CSRF_SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-XSRF-TOKEN")))
                .addSecurityItem(new SecurityRequirement().addList(CSRF_SECURITY_SCHEME));
    }

    @Bean
    public GroupedOpenApi restApi() {
        return GroupedOpenApi.builder()
                .group("rest")
                .pathsToMatch("/api/**")
                .build();
    }
}

