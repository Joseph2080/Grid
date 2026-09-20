package org.bazar.vektrlabs.ai.tool;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.cart.CartResponseDto;
import org.bazar.vektrlabs.dto.response.OrderResponseDto;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.facade.store.StoreAiFacade;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StoreTools {

    public static final String CART_ID = "cartId";
    private final StoreAiFacade storeAiFacade;

    @Tool(
            name = "get_store_products",
            description = "Get the products available in the current store, including their names, descriptions, prices, categories, variants and stock."
    )
    public List<ProductResponseDto> getStoreProducts(
            ToolContext toolContext) {
        UUID storeId = (UUID) toolContext.getContext().get("storeId");
        return storeAiFacade.findProducts(storeId);
    }

    @Tool(
            name = "get_product",
            description = "Get detailed information about a product, including its current price and available variants."
    )
    public ProductResponseDto getProduct(
            @ToolParam(description = "The product ID")
            UUID productId) {
        return storeAiFacade.findProduct(productId);
    }

    @Tool(
            name = "create_cart",
            description = "Create a new shopping cart and add the selected product variant with the requested quantity."
    )
    public CartResponseDto createCart(
            @ToolParam(description = "The product variant ID")
            UUID variantId,
            @ToolParam(description = "The quantity to add")
            Integer quantity,
            ToolContext toolContext) {
        return storeAiFacade.createCart(
                variantId,
                quantity
        );
    }

    @Tool(
            name = "get_cart",
            description = "Get the current shopping cart and its items."
    )
    public CartResponseDto getCart(
            ToolContext toolContext) {
        UUID cartId = (UUID) toolContext.getContext().get(CART_ID);
        return storeAiFacade.getCart(cartId);
    }

    @Tool(
            name = "add_to_cart",
            description = "Add a product variant to the customer's shopping cart."
    )
    public CartResponseDto addToCart(
            @ToolParam(description = "The product variant ID")
            UUID variantId,
            @ToolParam(description = "The quantity to add")
            Integer quantity,
            ToolContext toolContext) {
        UUID cartId = (UUID) toolContext.getContext().get(CART_ID);
        return storeAiFacade.addToCart(
                cartId,
                variantId,
                quantity
        );
    }

    @Tool(
            name = "checkout",
            description = "Create an order from the customer's current shopping cart and begin payment."
    )
    public OrderResponseDto checkout(
            ToolContext toolContext) {
        UUID cartId = (UUID) toolContext.getContext().get(CART_ID);
        return storeAiFacade.checkout(cartId);
    }

    @Tool(
            name = "get_order_status",
            description = "Get the current status and details of an order."
    )
    public OrderResponseDto getOrderStatus(
            @ToolParam(description = "The order ID")
            UUID orderId) {
        return storeAiFacade.getOrder(orderId);
    }
}