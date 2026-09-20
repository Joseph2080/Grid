package org.bazar.vektrlabs.directprofile;

import org.bazar.vektrlabs.controller.rest.OrderController;
import org.bazar.vektrlabs.controller.rest.StoreController;
import org.bazar.vektrlabs.controller.rest.UserProfileController;
import org.bazar.vektrlabs.dto.request.OrderRequestDto;
import org.bazar.vektrlabs.dto.request.StoreRequestDto;
import org.bazar.vektrlabs.dto.request.UserProfileRequestDto;
import org.bazar.vektrlabs.dto.response.OrderResponseDto;
import org.bazar.vektrlabs.dto.response.StoreResponseDto;
import org.bazar.vektrlabs.dto.response.UserProfileResponseDto;
import org.bazar.vektrlabs.facade.CheckoutFacade;
import org.bazar.vektrlabs.facade.store.StoreViewFacade;
import org.bazar.vektrlabs.service.StoreService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectProfileControllersTest {
    @Test
    void postOrderAcceptsOnlyCartIdAndUsesCheckoutFacade() throws Exception {
        var checkout = mock(CheckoutFacade.class);
        var cartId = UUID.randomUUID();
        when(checkout.checkout(cartId)).thenReturn(OrderResponseDto.builder().cartId(cartId).build());
        var mvc = MockMvcBuilders.standaloneSetup(new OrderController(checkout)).build();

        mvc.perform(post("/api/v1/order").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":\"" + cartId + "\"}"))
                .andExpect(status().isCreated());

        verify(checkout).checkout(cartId);
        assertEquals(List.of("cartId"), Arrays.stream(OrderRequestDto.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(java.lang.reflect.Field::getName).toList());
    }

    @Test
    void postOrderMissingCartIdIsRejectedBeforeCheckout() throws Exception {
        var checkout = mock(CheckoutFacade.class);
        var mvc = MockMvcBuilders.standaloneSetup(new OrderController(checkout)).build();

        mvc.perform(post("/api/v1/order").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(checkout);
    }

    @Test
    void getOrderUsesOwnershipAwareFacade() throws Exception {
        var checkout = mock(CheckoutFacade.class);
        var orderId = UUID.randomUUID();
        when(checkout.getOrder(orderId)).thenReturn(OrderResponseDto.builder().id(orderId).build());
        var mvc = MockMvcBuilders.standaloneSetup(new OrderController(checkout)).build();

        mvc.perform(get("/api/v1/order/" + orderId)).andExpect(status().isOk());

        verify(checkout).getOrder(orderId);
    }

    @Test
    void postStoreBindsExistingMultipartRequestDirectlyToStoreService() throws Exception {
        var stores = mock(StoreService.class);
        var views = mock(StoreViewFacade.class);
        when(stores.create(any(StoreRequestDto.class))).thenReturn(
                StoreResponseDto.builder().id(UUID.randomUUID()).name("My store").build());
        var mvc = MockMvcBuilders.standaloneSetup(new StoreController(stores, views)).build();
        var logo = new MockMultipartFile("logo", "brand.png", "image/png", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/v1/store").file(logo).param("name", "My store")
                        .param("description", "My description").param("twitterUrl", "https://example.test/store")
                        .param("currency", "USD"))
                .andExpect(status().isCreated());

        var request = ArgumentCaptor.forClass(StoreRequestDto.class);
        verify(stores).create(request.capture());
        assertEquals("My store", request.getValue().getName());
        assertEquals("My description", request.getValue().getDescription());
        assertEquals("https://example.test/store", request.getValue().getTwitterUrl());
        assertEquals(org.bazar.vektrlabs.entity.enums.Currency.USD, request.getValue().getCurrency());
        assertEquals("brand.png", request.getValue().getLogo().getOriginalFilename());
        assertArrayEquals(logo.getBytes(), request.getValue().getLogo().getBytes());
        verifyNoInteractions(views);
    }

    @Test
    void profileMeAndUpdateResolveIdentityAndProfileIdServerSide() {
        var profiles = mock(UserProfileService.class);
        var currentUser = mock(CurrentUserUtil.class);
        var profileId = UUID.randomUUID();
        var response = UserProfileResponseDto.builder().id(profileId).cognitoSub("owner").build();
        var request = new UserProfileRequestDto("First", "Last", null);
        when(currentUser.currentCognitoSub()).thenReturn("owner");
        when(profiles.findByCognitoSubOrElseThrowException("owner")).thenReturn(response);
        when(profiles.update(profileId, request)).thenReturn(response);
        var controller = new UserProfileController(profiles, currentUser);

        assertEquals(HttpStatus.OK, controller.me().getStatusCode());
        assertEquals(HttpStatus.OK, controller.update(request).getStatusCode());

        verify(profiles, times(2)).findByCognitoSubOrElseThrowException("owner");
        verify(profiles).update(profileId, request);
    }
}
