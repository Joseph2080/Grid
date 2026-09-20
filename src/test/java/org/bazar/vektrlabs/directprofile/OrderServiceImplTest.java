package org.bazar.vektrlabs.directprofile;

import org.bazar.vektrlabs.dto.request.OrderCommand;
import org.bazar.vektrlabs.dto.request.OrderItemRequestDto;
import org.bazar.vektrlabs.entity.Order;
import org.bazar.vektrlabs.entity.Product;
import org.bazar.vektrlabs.entity.ProductVariant;
import org.bazar.vektrlabs.entity.Store;
import org.bazar.vektrlabs.entity.UserProfile;
import org.bazar.vektrlabs.entity.enums.Currency;
import org.bazar.vektrlabs.entity.enums.OrderStatus;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.bazar.vektrlabs.mapper.OrderItemMapper;
import org.bazar.vektrlabs.mapper.OrderMapper;
import org.bazar.vektrlabs.repository.OrderRepository;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.service.impl.OrderServiceImpl;
import org.jericho.common.exception.InvalidParameterException;
import org.jericho.payment.dto.PaymentRequestDto;
import org.jericho.payment.dto.PaymentResponseDto;
import org.jericho.payment.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final ProductVariantService variants = mock(ProductVariantService.class);
    private final UserProfileService profiles = mock(UserProfileService.class);
    private final OrderMapper mapper = new OrderMapper(new OrderItemMapper());
    private final OrderServiceImpl service = new OrderServiceImpl(repository, mapper, payments, variants, profiles);
    private final UserProfile buyer = Fixtures.profile("buyer");
    private final UUID cartId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();
    private final Store store = buildStore(Currency.GBP);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        when(profiles.requireCompleteProfileByCognitoSub("buyer")).thenReturn(buyer);
        var product = new Product();
        product.setPrice(new BigDecimal("12.50"));
        product.setStore(store);
        var variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProduct(product);
        when(variants.findAvailableVariant(variantId, 2)).thenReturn(variant);
        when(repository.save(any(Order.class))).thenAnswer(invocation -> {
            Order entity = invocation.getArgument(0);
            assertSame(buyer, entity.getBuyer(), "Buyer must be assigned in dependency hook before save");
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            return entity;
        });
        var payment = mock(PaymentResponseDto.class);
        when(payment.getPaymentUrl()).thenReturn("https://example.test/payment");
        when(payments.createPayment(any(PaymentRequestDto.class), eq("STRIPE"))).thenReturn(payment);
        ReflectionTestUtils.setField(service, "PAYMENT_STRATEGY", "STRIPE");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUsesCommandBuyerWithoutSecurityContextAndPricesServerSide() {
        var response = service.create(command("buyer", cartId));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(buyer.getId(), response.getBuyerId());
        assertEquals(cartId, response.getCartId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("25.00"), response.getTotalAmount());
        assertEquals(Currency.GBP, response.getCurrency());
        assertEquals(new BigDecimal("12.50"), response.getItems().getFirst().getUnitPrice());
        assertEquals(variantId, response.getItems().getFirst().getVariantId());
        assertEquals("https://example.test/payment", response.getPaymentUrl());
        var order = ArgumentCaptor.forClass(Order.class);
        var payment = ArgumentCaptor.forClass(PaymentRequestDto.class);
        var inOrder = inOrder(profiles, variants, repository, payments);
        inOrder.verify(profiles).requireCompleteProfileByCognitoSub("buyer");
        inOrder.verify(variants).findAvailableVariant(variantId, 2);
        inOrder.verify(repository).save(order.capture());
        inOrder.verify(payments).createPayment(payment.capture(), eq("STRIPE"));
        assertSame(order.getValue(), order.getValue().getItems().getFirst().getOrder());
        assertEquals(new BigDecimal("25.00"), payment.getValue().getAmount());
        assertEquals("GBP", payment.getValue().getCurrency());
        assertEquals(response.getId().toString(), payment.getValue().getMetadata().get("orderId"));
        assertEquals(cartId.toString(), payment.getValue().getMetadata().get("cartId"));
    }

    @Test
    void ordersSpanningMultipleStoresAreRejected() {
        var otherStoreVariantId = UUID.randomUUID();
        var otherProduct = new Product();
        otherProduct.setPrice(new BigDecimal("5.00"));
        otherProduct.setStore(buildStore(Currency.USD));
        var otherVariant = new ProductVariant();
        otherVariant.setId(otherStoreVariantId);
        otherVariant.setProduct(otherProduct);
        when(variants.findAvailableVariant(otherStoreVariantId, 1)).thenReturn(otherVariant);
        var command = new OrderCommand(cartId, List.of(
                new OrderItemRequestDto(variantId, 2),
                new OrderItemRequestDto(otherStoreVariantId, 1)), "buyer");

        assertThrows(InvalidParameterException.class, () -> service.create(command));

        verify(repository, never()).save(any());
        verifyNoInteractions(payments);
    }

    @Test
    void unrelatedThreadAuthenticationDoesNotOverrideTrustedCommandBuyer() {
        Fixtures.signIn("unrelated-worker");

        var response = service.create(command("buyer", cartId));

        assertEquals(buyer.getId(), response.getBuyerId());
        verify(profiles).requireCompleteProfileByCognitoSub("buyer");
        verify(profiles, never()).requireCompleteProfileByCognitoSub("unrelated-worker");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void missingOrIncompleteBuyerPreventsPersistencePaymentAndVariantAccess(boolean missing) {
        RuntimeException failure = missing ? new ProfileNotFoundException("missing")
                : new ProfileIncompleteException("incomplete");
        when(profiles.requireCompleteProfileByCognitoSub("buyer")).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class, () -> service.create(command("buyer", cartId))));

        verify(repository, never()).save(any());
        verifyNoInteractions(payments, variants);
    }

    @ParameterizedTest
    @ValueSource(strings = {"buyer", "cart", "null-items", "empty-items", "null-item", "variant",
            "null-quantity", "zero-quantity", "negative-quantity"})
    void invalidCommandsFailBeforePersistencePaymentOrProfileLookup(String invalid) {
        var validItems = List.of(new OrderItemRequestDto(variantId, 2));
        var command = switch (invalid) {
            case "buyer" -> new OrderCommand(cartId, validItems, " ");
            case "cart" -> new OrderCommand(null, validItems, "buyer");
            case "null-items" -> new OrderCommand(cartId, null, "buyer");
            case "empty-items" -> new OrderCommand(cartId, List.of(), "buyer");
            case "null-item" -> new OrderCommand(cartId, java.util.Collections.singletonList(null), "buyer");
            case "variant" -> new OrderCommand(cartId, List.of(new OrderItemRequestDto(null, 2)), "buyer");
            case "null-quantity" -> new OrderCommand(cartId, List.of(new OrderItemRequestDto(variantId, null)), "buyer");
            case "zero-quantity" -> new OrderCommand(cartId, List.of(new OrderItemRequestDto(variantId, 0)), "buyer");
            case "negative-quantity" -> new OrderCommand(cartId, List.of(new OrderItemRequestDto(variantId, -1)), "buyer");
            default -> throw new IllegalArgumentException(invalid);
        };

        assertThrows(InvalidParameterException.class, () -> service.create(command));

        verify(repository, never()).save(any());
        verifyNoInteractions(profiles, payments, variants);
    }

    @Test
    void updateCannotReplaceBuyerOrMutateCartAndItemsFirst() {
        var order = existingOrder();
        var other = Fixtures.profile("other");
        when(profiles.requireCompleteProfileByCognitoSub("other")).thenReturn(other);
        when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> service.update(order.getId(), command("other", cartId)));

        assertSame(buyer, order.getBuyer());
        assertEquals(cartId, order.getCartId());
        assertTrue(order.getItems().isEmpty());
        verify(repository, never()).save(any());
        verifyNoInteractions(payments, variants);
    }

    @Test
    void updateCannotReplaceCart() {
        var order = existingOrder();
        when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(InvalidParameterException.class,
                () -> service.update(order.getId(), command("buyer", UUID.randomUUID())));

        assertEquals(cartId, order.getCartId());
        assertSame(buyer, order.getBuyer());
        verify(repository, never()).save(any());
        verifyNoInteractions(payments, variants);
    }

    @Test
    void normalUpdateRetainsBuyerCartAndExistingStatusWithoutCreatingPayment() {
        var order = existingOrder();
        var status = OrderStatus.PAID;
        order.setStatus(status);
        when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        var response = service.update(order.getId(), command("buyer", cartId));

        assertSame(buyer, order.getBuyer());
        assertEquals(cartId, response.getCartId());
        assertEquals(status, response.getStatus());
        assertEquals(new BigDecimal("25.00"), response.getTotalAmount());
        verify(repository).save(order);
        verifyNoInteractions(payments);
    }

    @Test
    void mapperDoesNotResolveBuyerOrResetExistingStatus() {
        var created = mapper.convertDtoToEntity(command("buyer", cartId));
        assertEquals(cartId, created.getCartId());
        assertNull(created.getBuyer());
        assertTrue(created.getItems().isEmpty());
        assertEquals(OrderStatus.PENDING, created.getStatus());
        var existing = existingOrder();
        var status = OrderStatus.PAID;
        existing.setStatus(status);

        mapper.updateEntityFromDto(command("other", cartId), existing);

        assertSame(buyer, existing.getBuyer());
        assertEquals(status, existing.getStatus());
        assertEquals(buyer.getId(), mapper.convertEntityToResponseDto(existing).getBuyerId());
    }

    @Test
    void paymentStatusUpdateWorksOnUnauthenticatedAsyncWorker() throws Exception {
        var order = existingOrder();
        when(repository.findById(order.getId())).thenReturn(Optional.of(order));
        var status = OrderStatus.PAID;
        Fixtures.signIn("request-not-propagated");

        try (var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> {
                assertNull(SecurityContextHolder.getContext().getAuthentication());
                return service.updateOrderStatus(order.getId(), status);
            }).get(5, TimeUnit.SECONDS);

            assertEquals(status, result.getStatus());
            assertEquals(buyer.getId(), result.getBuyerId());
            assertNotNull(result.getUpdatedAt());
        }
        verify(repository).save(order);
        verifyNoInteractions(profiles, payments, variants);
    }

    private OrderCommand command(String sub, UUID commandCartId) {
        return new OrderCommand(commandCartId, List.of(new OrderItemRequestDto(variantId, 2)), sub);
    }

    private Order existingOrder() {
        var order = new Order();
        order.setId(UUID.randomUUID());
        order.setBuyer(buyer);
        order.setCartId(cartId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);
        return order;
    }

    private Store buildStore(Currency currency) {
        var store = new Store();
        store.setId(UUID.randomUUID());
        store.setCurrency(currency);
        return store;
    }
}
