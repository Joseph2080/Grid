package org.bazar.vektrlabs.config;

import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class SecurityToolExecutionExceptionProcessor implements ToolExecutionExceptionProcessor {

    private final ToolExecutionExceptionProcessor delegate = new DefaultToolExecutionExceptionProcessor(false);

    @Override
    public String process(ToolExecutionException exception) {
        Throwable cause = unwrap(exception);
        if (cause instanceof AuthenticationException
                || cause instanceof AccessDeniedException
                || cause instanceof ProfileNotFoundException) {
            // Security failures must reach MVC, not become conversational tool output.
            throw exception;
        }
        return delegate.process(exception);
    }

    public static Throwable unwrap(ToolExecutionException exception) {
        Throwable cause = exception;
        while (cause instanceof ToolExecutionException wrapper && wrapper.getCause() != null) {
            cause = wrapper.getCause();
        }
        return cause;
    }
}
