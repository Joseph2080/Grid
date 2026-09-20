package org.bazar.vektrlabs.directprofile;

import org.bazar.vektrlabs.cart.CartItemRequestDto;
import org.bazar.vektrlabs.cart.CartRequestDto;
import org.bazar.vektrlabs.cart.CartService;
import org.bazar.vektrlabs.controller.rest.OrderController;
import org.bazar.vektrlabs.controller.rest.StoreController;
import org.bazar.vektrlabs.controller.rest.UserProfileController;
import org.bazar.vektrlabs.dto.request.OrderRequestDto;
import org.bazar.vektrlabs.dto.request.StoreRequestDto;
import org.bazar.vektrlabs.entity.Order;
import org.bazar.vektrlabs.entity.Product;
import org.bazar.vektrlabs.entity.ProductVariant;
import org.bazar.vektrlabs.entity.Store;
import org.bazar.vektrlabs.entity.enums.Currency;
import org.bazar.vektrlabs.facade.CheckoutFacade;
import org.bazar.vektrlabs.facade.store.StoreViewFacade;
import org.bazar.vektrlabs.mapper.OrderItemMapper;
import org.bazar.vektrlabs.mapper.OrderMapper;
import org.bazar.vektrlabs.mapper.StoreMapper;
import org.bazar.vektrlabs.mapper.UserProfileMapper;
import org.bazar.vektrlabs.repository.OrderRepository;
import org.bazar.vektrlabs.repository.StoreRepository;
import org.bazar.vektrlabs.repository.UserProfileRepository;
import org.bazar.vektrlabs.service.OrderService;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.bazar.vektrlabs.service.StoreService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.service.impl.OrderServiceImpl;
import org.bazar.vektrlabs.service.impl.StoreServiceImpl;
import org.bazar.vektrlabs.service.impl.UserProfileServiceImpl;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.mediaresource.service.MediaResourceService;
import org.jericho.payment.dto.PaymentRequestDto;
import org.jericho.payment.dto.PaymentResponseDto;
import org.jericho.payment.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DirectProfileContextTest {
    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final ProductVariantService variants = mock(ProductVariantService.class);
    private final MediaResourceService media = mock(MediaResourceService.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final StoreViewFacade views = mock(StoreViewFacade.class);
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void startScopedApplicationContext() {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test", Map.of("aws.s3.bucket", "test-bucket", "payment.strategy", "STRIPE")));
        context.registerBean(UserProfileRepository.class, () -> profiles);
        context.registerBean(StoreRepository.class, () -> stores);
        context.registerBean(OrderRepository.class, () -> orders);
        context.registerBean(ProductVariantService.class, () -> variants);
        context.registerBean(MediaResourceService.class, () -> media);
        context.registerBean(PaymentService.class, () -> payments);
        context.registerBean(StoreViewFacade.class, () -> views);
        context.register(CurrentUserUtil.class, UserProfileMapper.class, StoreMapper.class,
                OrderItemMapper.class, OrderMapper.class, UserProfileServiceImpl.class,
                StoreServiceImpl.class, OrderServiceImpl.class, CartService.class, CheckoutFacade.class,
                UserProfileController.class, StoreController.class, OrderController.class);
        context.refresh();
    }

    @AfterEach
    void stopContext() {
        SecurityContextHolder.clearContext();
        if (context != null) {
            context.close();
        }
    }

    @Test
    void constructorGraphStartsWithOnlyMockedPersistenceAndExternalIntegrations() {
        assertTrue(context.isActive());
        assertInstanceOf(UserProfileServiceImpl.class, context.getBean(UserProfileService.class));
        assertInstanceOf(StoreServiceImpl.class, context.getBean(StoreService.class));
        assertInstanceOf(OrderServiceImpl.class, context.getBean(OrderService.class));
        assertNotNull(context.getBean(CartService.class));
        assertNotNull(context.getBean(CheckoutFacade.class));
        assertNotNull(context.getBean(UserProfileController.class));
        assertNotNull(context.getBean(StoreController.class));
        assertNotNull(context.getBean(OrderController.class));
        assertEquals(1, context.getBeansOfType(UserProfileService.class).size());
        assertEquals(1, context.getBeansOfType(CurrentUserUtil.class).size());
        verifyNoInteractions(profiles, stores, orders, variants, media, payments, views);
    }

    @Test
    void directStoreControllerUsesSharedProfileServiceAndClaimIdentity() {
        Fixtures.signIn("owner");
        var profile = Fixtures.profile("owner");
        when(profiles.findByCognitoSub("owner")).thenReturn(Optional.of(profile));
        when(stores.save(any(Store.class))).thenAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            assertSame(profile, store.getUserProfile());
            store.setId(UUID.randomUUID());
            return store;
        });
        var controller = context.getBean(StoreController.class);

        for (String name : new String[]{"First store", "Second store"}) {
            var request = StoreRequestDto.builder().name(name)
                        .currency(Currency.USD)
                        .logo(new MockMultipartFile("logo", new byte[0])).build();
            assertEquals(HttpStatus.CREATED, controller.create(request).getStatusCode());
        }

        var saved = ArgumentCaptor.forClass(Store.class);
        verify(stores, times(2)).save(saved.capture());
        assertNotEquals(saved.getAllValues().getFirst().getId(), saved.getAllValues().getLast().getId());
        assertTrue(saved.getAllValues().stream().allMatch(store -> store.getUserProfile() == profile));
        verify(profiles, times(2)).findByCognitoSub("owner");
        verifyNoInteractions(orders, variants, media, payments, views);
    }

    @Test
    void cartOnlyOrderControllerTraversesRealCheckoutAndOrderServices() {
        Fixtures.signIn("buyer");
        var profile = Fixtures.profile("buyer");
        when(profiles.findByCognitoSub("buyer")).thenReturn(Optional.of(profile));
        var product = new Product();
        product.setPrice(new BigDecimal("7.50"));
        var store = new Store();
        store.setId(UUID.randomUUID());
        store.setCurrency(Currency.EUR);
        product.setStore(store);
        var variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(product);
        when(variants.findAvailableVariant(variant.getId(), 2)).thenReturn(variant);
        when(orders.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            assertSame(profile, order.getBuyer());
            order.setId(UUID.randomUUID());
            return order;
        });
        var payment = mock(PaymentResponseDto.class);
        when(payment.getPaymentUrl()).thenReturn("https://example.test/payment");
        when(payments.createPayment(any(PaymentRequestDto.class), eq("STRIPE"))).thenReturn(payment);
        var cart = context.getBean(CartService.class)
                .createCart(new CartRequestDto(new CartItemRequestDto(variant.getId(), 2)));

        var response = context.getBean(OrderController.class).create(new OrderRequestDto(cart.getId()));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        var saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        assertSame(profile, saved.getValue().getBuyer());
        assertEquals(cart.getId(), saved.getValue().getCartId());
        assertEquals(new BigDecimal("15.00"), saved.getValue().getTotalAmount());
        assertEquals(Currency.EUR, saved.getValue().getCurrency());
        assertEquals(variant.getId(), saved.getValue().getItems().getFirst().getVariant().getId());
        var paymentRequest = ArgumentCaptor.forClass(PaymentRequestDto.class);
        verify(payments).createPayment(paymentRequest.capture(), eq("STRIPE"));
        assertEquals("EUR", paymentRequest.getValue().getCurrency());
        verifyNoInteractions(stores, media, views);
    }
}
