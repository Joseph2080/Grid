package org.bazar.vektrlabs.directprofile;

import org.bazar.vektrlabs.ai.dto.response.ToolResponseType;
import org.bazar.vektrlabs.ai.tool.ToolExecutionCallback;
import org.bazar.vektrlabs.ai.tool.ToolExecutionCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ToolExecutionCallbackTest {
    private final ToolCallback delegate = mock(ToolCallback.class);
    private final ToolExecutionCollector collector = new ToolExecutionCollector();
    private final ToolDefinition definition = ToolDefinition.builder()
            .name("checkout").description("Checkout").inputSchema("{\"type\":\"object\"}").build();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void capturesRequestAuthenticationAndRestoresWorkerWithoutLeakingIdentity(boolean contextual) throws Exception {
        Fixtures.signIn("request-owner");
        var requestAuthentication = SecurityContextHolder.getContext().getAuthentication();
        var requestContext = SecurityContextHolder.getContext();
        var callback = new ToolExecutionCallback(delegate, collector);
        when(delegate.getToolDefinition()).thenReturn(definition);
        String input = "{\"cartId\":\"opaque-cart-id\"}";
        String output = "{\"paymentUrl\":\"https://example.test/payment\"}";
        var toolContext = new ToolContext(Map.of("traceId", "trace"));
        if (contextual) {
            when(delegate.call(input, toolContext)).thenAnswer(invocation -> {
                assertSame(requestAuthentication, SecurityContextHolder.getContext().getAuthentication());
                assertEquals(Map.of("traceId", "trace"), toolContext.getContext());
                return output;
            });
        } else {
            when(delegate.call(input)).thenAnswer(invocation -> {
                assertSame(requestAuthentication, SecurityContextHolder.getContext().getAuthentication());
                return output;
            });
        }

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                Fixtures.signIn("worker-owner");
                var previous = SecurityContextHolder.getContext();
                assertEquals(output, contextual ? callback.call(input, toolContext) : callback.call(input));
                assertSame(previous, SecurityContextHolder.getContext());
                assertEquals("worker-owner",
                        new org.bazar.vektrlabs.util.CurrentUserUtil().currentCognitoSub());
            }).get(5, TimeUnit.SECONDS);
        }

        assertSame(requestContext, SecurityContextHolder.getContext());
        assertSame(requestAuthentication, SecurityContextHolder.getContext().getAuthentication());
        assertSame(definition, callback.getToolDefinition());
        assertEquals(1, collector.getResponses().size());
        assertEquals(ToolResponseType.CHECKOUT, collector.getResponses().getFirst().type());
        assertEquals(output, collector.getResponses().getFirst().data());
        if (contextual) {
            verify(delegate).call(input, toolContext);
        } else {
            verify(delegate).call(input);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void delegateExceptionStillRestoresWorkerAndCollectsNothing(boolean contextual) throws Exception {
        Fixtures.signIn("request-owner");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var callback = new ToolExecutionCallback(delegate, collector);
        var failure = new IllegalStateException("delegate failed");
        var context = new ToolContext(Map.of());
        if (contextual) {
            when(delegate.call("{}", context)).thenAnswer(invocation -> {
                assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
                throw failure;
            });
        } else {
            when(delegate.call("{}")).thenAnswer(invocation -> {
                assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
                throw failure;
            });
        }

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                Fixtures.signIn("worker-owner");
                var previous = SecurityContextHolder.getContext();
                assertSame(failure, assertThrows(IllegalStateException.class,
                        () -> {
                            if (contextual) {
                                callback.call("{}", context);
                            } else {
                                callback.call("{}");
                            }
                        }));
                assertSame(previous, SecurityContextHolder.getContext());
            }).get(5, TimeUnit.SECONDS);
        }
        assertTrue(collector.getResponses().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void anonymousRequestCannotBorrowPreAuthenticatedWorkerIdentity(boolean anonymousToken) throws Exception {
        SecurityContextHolder.clearContext();
        Authentication requestAuthentication = anonymousToken
                ? new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))
                : null;
        SecurityContextHolder.getContext().setAuthentication(requestAuthentication);
        var callback = new ToolExecutionCallback(delegate, collector);
        when(delegate.getToolDefinition()).thenReturn(definition);
        when(delegate.call("{}")).thenAnswer(invocation -> {
            assertSame(requestAuthentication, SecurityContextHolder.getContext().getAuthentication());
            assertThrows(org.springframework.security.authentication.InsufficientAuthenticationException.class,
                    () -> new org.bazar.vektrlabs.util.CurrentUserUtil().currentCognitoSub());
            return "{}";
        });

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                Fixtures.signIn("worker-owner");
                var previous = SecurityContextHolder.getContext();
                callback.call("{}");
                assertSame(previous, SecurityContextHolder.getContext());
            }).get(5, TimeUnit.SECONDS);
        }
        assertEquals("{}", collector.getResponses().getFirst().data());
    }

    @Test
    void capturedAuthenticationIsPrivateFinalState() throws Exception {
        var field = ToolExecutionCallback.class.getDeclaredField("requestAuthentication");
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
        assertEquals(Authentication.class, field.getType());
    }
}
