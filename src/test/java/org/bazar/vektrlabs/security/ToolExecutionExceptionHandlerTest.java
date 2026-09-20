package org.bazar.vektrlabs.security;

import org.bazar.vektrlabs.exception.GlobalExceptionHandler;
import org.bazar.vektrlabs.config.SecurityToolExecutionExceptionProcessor;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ToolExecutionExceptionHandlerTest {

    private static final ToolDefinition TOOL = ToolDefinition.builder()
            .name("create_cart").description("Create a cart").inputSchema("{}").build();

    @Test
    void toolAuthenticationFailureRemainsJson401() throws Exception {
        request(new InsufficientAuthenticationException("Missing identity"), 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void toolMissingProfileRemainsJson404() throws Exception {
        request(new ProfileNotFoundException("Missing profile"), 404, "PROFILE_NOT_FOUND");
    }

    @Test
    void toolIncompleteProfileRemainsDistinctFromOwnershipFailure() throws Exception {
        request(new ProfileIncompleteException("Incomplete profile"), 403, "PROFILE_INCOMPLETE");
        request(new AccessDeniedException("Other buyer"), 403, "ACCESS_DENIED");
    }

    @Test
    void nestedToolWrappersStillPreserveProfileReason() throws Exception {
        request(new ToolExecutionException(TOOL, new ProfileNotFoundException("Missing")), 404, "PROFILE_NOT_FOUND");
    }

    @Test
    void unrelatedToolFailuresRemainGenericAndDoNotExposeDetails() throws Exception {
        mvc(new IllegalStateException("Internal tool details"))
                .perform(post("/api/v1/ai/chat"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errors.code").doesNotExist())
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred. Please try again later."));
    }

    @Test
    void onlyKnownToolWrappersAreUnwrapped() throws Exception {
        mvc(new IllegalStateException("Unrelated wrapper", new ProfileNotFoundException("Missing")))
                .perform(post("/api/v1/ai/chat"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errors.code").doesNotExist());
    }

    @Test
    void aiExceptionProcessorPropagatesSecurityFailuresInsteadOfSendingThemToTheModel() {
        var processor = new SecurityToolExecutionExceptionProcessor();
        for (RuntimeException cause : new RuntimeException[]{
                new InsufficientAuthenticationException("Missing identity"),
                new ProfileNotFoundException("Missing profile"),
                new ProfileIncompleteException("Incomplete profile"),
                new AccessDeniedException("Other buyer"),
                new ToolExecutionException(TOOL, new ProfileNotFoundException("Nested"))
        }) {
            var wrapper = new ToolExecutionException(TOOL, cause);
            assertSame(wrapper, assertThrows(ToolExecutionException.class, () -> processor.process(wrapper)));
        }
    }

    @Test
    void aiExceptionProcessorPreservesNormalNonSecurityErrorProcessing() {
        var wrapper = new ToolExecutionException(TOOL, new IllegalArgumentException("Invalid product choice"));
        assertEquals(wrapper.getMessage(), new SecurityToolExecutionExceptionProcessor().process(wrapper));
    }

    @Test
    void aiAutoConfigurationUsesTheSecurityProcessor() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ToolCallingAutoConfiguration.class))
                .withUserConfiguration(SecurityToolExecutionExceptionProcessor.class)
                .run(context -> assertInstanceOf(SecurityToolExecutionExceptionProcessor.class,
                        context.getBean(ToolExecutionExceptionProcessor.class)));
    }

    private void request(Throwable cause, int status, String code) throws Exception {
        mvc(cause).perform(post("/api/v1/ai/chat")).andExpect(status().is(status))
                .andExpect(jsonPath("$.errors.code").value(code));
    }

    private MockMvc mvc(Throwable cause) {
        return MockMvcBuilders.standaloneSetup(new ToolController(cause))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @RestController
    static class ToolController {
        private final Throwable failure;

        ToolController(Throwable failure) {
            this.failure = failure;
        }

        @PostMapping("/api/v1/ai/chat")
        void chat() {
            throw new ToolExecutionException(TOOL, failure);
        }
    }
}
