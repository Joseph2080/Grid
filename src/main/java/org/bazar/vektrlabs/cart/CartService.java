package org.bazar.vektrlabs.cart;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.common.exception.InvalidParameterException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private static final long CART_TTL_SECONDS = 600;
    private static final long CART_MAX_ENTRIES = 100_000;

    /**
     * Caffeine-backed store: bounds memory usage and provides a passive
     * expiry safety net (matching CART_TTL_SECONDS) in case a cart is
     * abandoned and never explicitly cleared/expired via getOwnedCartEntity.
     */
    private final Map<UUID, Cart> carts = Caffeine.newBuilder()
            .expireAfterWrite(CART_TTL_SECONDS, TimeUnit.SECONDS)
            .maximumSize(CART_MAX_ENTRIES)
            .<UUID, Cart>build()
            .asMap();
    private final Map<UUID, Integer> reservedItems = new ConcurrentHashMap<>();
    private final ProductVariantService productVariantService;
    private final CurrentUserUtil currentUserUtil;
    private final UserProfileService userProfileService;

    public CartResponseDto createCart(CartRequestDto cartRequestDto) {
        String buyerCognitoSub = currentBuyerSub();
        if (cartRequestDto == null) {
            throw new InvalidParameterException("Cart request is required.");
        }
        validateItem(cartRequestDto.getItem());
        log.info("Creating cart for buyerCognitoSub={}, variantId={}, quantity={}",
                buyerCognitoSub,
                cartRequestDto.getItem().getVariantId(),
                cartRequestDto.getItem().getQuantity());
        Cart cart = Cart.builder()
                .id(UUID.randomUUID())
                .buyerCognitoSub(buyerCognitoSub)
                .items(new ArrayList<>())
                .expiresAt(Instant.now().plusSeconds(CART_TTL_SECONDS))
                .build();
        addCartItemToList(cart.getItems(), cartRequestDto.getItem());
        carts.put(cart.getId(), cart);
        log.info("Cart created successfully. cartId={}, itemCount={}",
                cart.getId(),
                cart.getItems().size());
        return CartUtil.toCartResponseDto(cart);
    }

    public CartResponseDto getCart(UUID cartId) {
        log.debug("Retrieving cart. cartId={}", cartId);
        Cart cart = getOwnedCartEntity(cartId);
        CartResponseDto response = CartUtil.toCartResponseDto(cart);
        log.debug("Cart retrieved successfully. cartId={}", cartId);
        return response;
    }

    public CartResponseDto addItem(UUID cartId, CartItemRequestDto itemDto) {
        var cart = getOwnedCartEntity(cartId);
        validateItem(itemDto);
        log.info("Adding item to cart. cartId={}, variantId={}, quantity={}",
                cartId,
                itemDto.getVariantId(),
                itemDto.getQuantity());
        addCartItemToList(cart.getItems(), itemDto);
        log.info("Item added to cart successfully. cartId={}, variantId={}",
                cartId,
                itemDto.getVariantId());
        return CartUtil.toCartResponseDto(cart);
    }

    private void addCartItemToList(List<CartItem> cartItems, CartItemRequestDto itemRequestDto) {
        var requestQuantity = itemRequestDto.getQuantity();
        var variantId = itemRequestDto.getVariantId();
        log.debug("Adding cart item. variantId={}, quantity={}",
                variantId,
                requestQuantity);
        reserveStock(variantId, requestQuantity);
        cartItems.stream()
                .filter(item -> item.getVariantId().equals(variantId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            int oldQuantity = existing.getQuantity();
                            existing.setQuantity(existing.getQuantity() + requestQuantity);

                            log.debug("Updated existing cart item. variantId={}, oldQuantity={}, newQuantity={}",
                                    variantId,
                                    oldQuantity,
                                    existing.getQuantity());
                        },
                        () -> {
                            cartItems.add(CartUtil.toCartItem(itemRequestDto));

                            log.debug("Added new cart item. variantId={}, quantity={}",
                                    variantId,
                                    requestQuantity);
                        });
    }

    public CartResponseDto updateQuantity(UUID cartId, UUID variantId, Integer quantity) {
        log.info("Updating cart item quantity. cartId={}, variantId={}, quantity={}",
                cartId,
                variantId,
                quantity);
        Cart cart = getOwnedCartEntity(cartId);
        if (quantity == null || quantity < 1) {
            throw new InvalidParameterException("Quantity must be at least one.");
        }
        cart.getItems().stream()
                .filter(i -> i.getVariantId().equals(variantId))
                .findFirst()
                .ifPresent(i -> {
                    int oldQuantity = i.getQuantity();
                    int delta = quantity - oldQuantity;
                    log.debug("Cart quantity delta calculated. cartId={}, variantId={}, oldQuantity={}, newQuantity={}, delta={}",
                            cartId,
                            variantId,
                            oldQuantity,
                            quantity,
                            delta);
                    if (delta > 0) {
                        reserveStock(variantId, delta);
                    } else if (delta < 0) {
                        releaseStock(variantId, -delta);
                    }
                    i.setQuantity(quantity);
                });
        log.info("Cart item quantity updated. cartId={}, variantId={}, quantity={}",
                cartId,
                variantId,
                quantity);
        return CartUtil.toCartResponseDto(cart);
    }

    public CartResponseDto removeItem(UUID cartId, UUID variantId) {
        log.info("Removing cart item. cartId={}, variantId={}",
                cartId,
                variantId);
        Cart cart = getOwnedCartEntity(cartId);
        cart.getItems().stream()
                .filter(i -> i.getVariantId().equals(variantId))
                .findFirst()
                .ifPresent(i -> {
                    releaseStock(variantId, i.getQuantity());
                    cart.getItems().remove(i);
                    log.debug("Cart item removed. cartId={}, variantId={}, quantity={}",
                            cartId,
                            variantId,
                            i.getQuantity());
                });
        log.info("Cart item removal completed. cartId={}, variantId={}",
                cartId,
                variantId);
        return CartUtil.toCartResponseDto(cart);
    }

    public void clear(UUID cartId) {
        log.info("Clearing cart. cartId={}", cartId);
        clearInternal(cartId, getOwnedCartEntity(cartId));
    }

    /**
     * Used by trusted internal callers only (e.g. OrderPaymentEventConsumer
     * after a successful payment webhook), where there is no authenticated
     * request/SecurityContext available to check ownership against.
     */
    public void clearAsSystem(UUID cartId) {
        log.info("Clearing cart as system. cartId={}", cartId);
        Cart cart = carts.get(cartId);
        if (cart != null) {
            clearInternal(cartId, cart);
        } else {
            log.debug("Cart not found while clearing as system. cartId={}", cartId);
        }
    }

    private void clearInternal(UUID cartId, Cart cart) {
        if (carts.remove(cartId, cart)) {
            cart.getItems().forEach(item -> releaseStock(item.getVariantId(), item.getQuantity()));
            log.info("Cart cleared successfully. cartId={}, itemCount={}",
                    cartId,
                    cart.getItems().size());
        } else {
            log.debug("Cart already cleared. cartId={}", cartId);
        }
    }

    /**
     * Lock-free thread-safe reservation using Compare-and-Swap (CAS).
     * Prevents blocking the ConcurrentHashMap during a database call.
     */
    private void reserveStock(UUID variantId, int additionalQuantity) {
        log.debug("Starting stock reservation. variantId={}, additionalQuantity={}",
                variantId,
                additionalQuantity);
        int attempts = 0;
        while (true) {
            attempts++;
            int currentReserved = reservedItems.getOrDefault(variantId, 0);
            int totalNeeded = currentReserved + additionalQuantity;
            log.debug("Checking stock availability. variantId={}, attempt={}, currentReserved={}, additionalQuantity={}, totalNeeded={}",
                    variantId,
                    attempts,
                    currentReserved,
                    additionalQuantity,
                    totalNeeded);
            // 1. Fetch from DB lock-free. Throws exception if stock is unavailable.
            productVariantService.findAvailableVariant(variantId, totalNeeded);
            if (currentReserved == 0) {
                if (reservedItems.putIfAbsent(variantId, totalNeeded) == null) {
                    log.info("Stock reserved successfully. variantId={}, reservedQuantity={}, attempts={}",
                            variantId,
                            totalNeeded,
                            attempts);
                    return;
                }
                log.debug("Reservation CAS failed on insert. Retrying. variantId={}, attempt={}",
                        variantId,
                        attempts);
            } else {
                if (reservedItems.replace(variantId, currentReserved, totalNeeded)) {
                    log.info("Stock reservation updated successfully. variantId={}, previousReserved={}, newReserved={}, attempts={}",
                            variantId,
                            currentReserved,
                            totalNeeded,
                            attempts);
                    return;
                }
                log.debug("Reservation CAS failed on replace. Retrying. variantId={}, attempt={}",
                        variantId,
                        attempts);
            }
            // If we reach here, another thread updated the reservation concurrently. We loop and retry.
        }
    }

    /**
     * Lock-free thread-safe release using Compare-and-Swap.
     */
    private void releaseStock(UUID variantId, int quantityToRelease) {
        log.debug("Starting stock release. variantId={}, quantityToRelease={}",
                variantId,
                quantityToRelease);
        int attempts = 0;
        while (true) {
            attempts++;
            Integer currentReserved = reservedItems.get(variantId);
            if (currentReserved == null) {
                log.debug("No reserved stock found for variant. variantId={}, attempts={}",
                        variantId,
                        attempts);
                return;
            }
            int newReserved = currentReserved - quantityToRelease;
            if (newReserved <= 0) {
                if (reservedItems.remove(variantId, currentReserved)) {
                    log.info("Stock reservation completely released. variantId={}, releasedQuantity={}, attempts={}",
                            variantId,
                            quantityToRelease,
                            attempts);
                    return;
                }
                log.debug("Reservation CAS failed on remove. Retrying. variantId={}, attempt={}",
                        variantId,
                        attempts);
            } else {
                if (reservedItems.replace(variantId, currentReserved, newReserved)) {
                    log.info("Stock reservation partially released. variantId={}, previousReserved={}, newReserved={}, attempts={}",
                            variantId,
                            currentReserved,
                            newReserved,
                            attempts);
                    return;
                }
                log.debug("Reservation CAS failed on replace during release. Retrying. variantId={}, attempt={}",
                        variantId,
                        attempts);
            }
        }
    }

    private Cart getCartEntity(UUID cartId) {
        log.debug("Looking up cart. cartId={}", cartId);
        Cart cart = carts.get(cartId);
        if (cart == null) {
            log.warn("Cart not found. cartId={}", cartId);
            throw new CartNotFoundException("Cart not found.");
        }
        return cart;
    }

    /**
     * Looks up a cart and asserts it belongs to the currently authenticated
     * user, so one user cannot read/mutate another user's cart by guessing
     * its UUID. Enforced here (not just at controller/security-filter
     * level) so AI tool calls (StoreTools) are gated identically.
     */
    private Cart getOwnedCartEntity(UUID cartId) {
        String currentCognitoSub = currentBuyerSub();
        if (cartId == null) {
            throw new InvalidParameterException("Cart ID is required.");
        }
        Cart cart = getCartEntity(cartId);
        if (!currentCognitoSub.equals(cart.getBuyerCognitoSub())) {
            log.warn("Cart ownership check failed. cartId={}", cartId);
            throw new AccessDeniedException("This cart does not belong to the signed-in user.");
        }
        if (isExpired(cart.getExpiresAt())) {
            log.info("Cart expired. cartId={}, expiresAt={}", cartId, cart.getExpiresAt());
            clearAsSystem(cartId);
            throw new CartNotFoundException("Cart has expired.");
        }
        return cart;
    }

    private String currentBuyerSub() {
        return userProfileService.requireCompleteProfileByCognitoSub(currentUserUtil.currentCognitoSub())
                .getCognitoSub();
    }

    private void validateItem(CartItemRequestDto item) {
        if (item == null || item.getVariantId() == null || item.getQuantity() == null || item.getQuantity() < 1) {
            throw new InvalidParameterException("A variant ID and quantity of at least one are required.");
        }
    }

    private boolean isExpired(Instant expiresAt) {
        boolean expired = Instant.now().isAfter(expiresAt);
        log.trace("Cart expiration check. expiresAt={}, expired={}",
                expiresAt,
                expired);
        return expired;
    }
}