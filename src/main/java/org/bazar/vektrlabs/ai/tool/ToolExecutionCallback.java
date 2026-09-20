package org.bazar.vektrlabs.ai.tool;

import org.bazar.vektrlabs.ai.dto.response.ToolResponseType;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

public class ToolExecutionCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolExecutionCollector collector;
    private final Authentication requestAuthentication;

    public ToolExecutionCallback(
            ToolCallback delegate,
            ToolExecutionCollector collector) {

        this.delegate = delegate;
        this.collector = collector;
        this.requestAuthentication = SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return invoke(() -> delegate.call(toolInput));
    }

    @Override
    public String call(
            String toolInput,
            ToolContext toolContext) {
        return invoke(() -> delegate.call(toolInput, toolContext));
    }

    private String invoke(Supplier<String> invocation) {
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(requestAuthentication);
        // Model tools may execute on a worker thread; never use that thread's caller.
        SecurityContextHolder.setContext(context);
        try {
            String result = invocation.get();
            collect(result);
            return result;
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private void collect(String result) {
        collector.add(
                resolveType(
                        delegate.getToolDefinition().name()
                ),
                result
        );
    }

    private ToolResponseType resolveType(
            String toolName) {

        return switch (toolName) {
            case "get_store_products" ->
                    ToolResponseType.PRODUCTS;

            case "get_product" ->
                    ToolResponseType.PRODUCT;

            case "get_cart" ->
                    ToolResponseType.CART;

            case "create_cart" ->
                    ToolResponseType.CART_UPDATED;

            case "add_to_cart" ->
                    ToolResponseType.CART_UPDATED;

            case "checkout" ->
                    ToolResponseType.CHECKOUT;

            case "get_order_status" ->
                    ToolResponseType.ORDER_STATUS;

            default ->
                    throw new IllegalArgumentException(
                            "Unknown tool: " + toolName
                    );
        };
    }
}