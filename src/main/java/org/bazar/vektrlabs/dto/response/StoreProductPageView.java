package org.bazar.vektrlabs.dto.response;

/**
 * Combines the paginated storefront view with a single resolved product,
 * for the /{slug}/product/{productId} deep-link page. {@code selectedProduct}
 * is {@code null} when the product doesn't exist or doesn't belong to the
 * resolved store.
 */
public record StoreProductPageView(
        StoreViewResponse storeView,
        ProductResponseDto selectedProduct
) {
}
