package org.bazar.vektrlabs.directprofile;

import org.bazar.vektrlabs.controller.page.ProductPageController;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.dto.response.StoreProductPageView;
import org.bazar.vektrlabs.dto.response.StoreResponseDto;
import org.bazar.vektrlabs.dto.response.StoreViewResponse;
import org.bazar.vektrlabs.facade.store.StoreViewFacade;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ProductPageControllerTest {

    private StoreViewResponse storeView(UUID storeId) {
        return new StoreViewResponse(
                StoreResponseDto.builder().id(storeId).name("My store").build(),
                List.of(), List.of(), List.of(), 1, 12, 0, 1, false, false);
    }

    @Test
    void productFoundInStoreIsExposedAsSelectedProduct() throws Exception {
        var storeViews = mock(StoreViewFacade.class);
        var storeId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var product = ProductResponseDto.builder().id(productId).storeId(storeId).name("Widget").build();

        when(storeViews.getViewWithSelectedProduct("my-store", productId))
                .thenReturn(new StoreProductPageView(storeView(storeId), product));

        var mvc = MockMvcBuilders.standaloneSetup(new ProductPageController(storeViews)).build();

        mvc.perform(get("/my-store/product/" + productId))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("selectedProduct", product));
    }

    @Test
    void productNotInStoreFallsBackToPlainStoreRender() throws Exception {
        var storeViews = mock(StoreViewFacade.class);
        var storeId = UUID.randomUUID();
        var productId = UUID.randomUUID();

        when(storeViews.getViewWithSelectedProduct("my-store", productId))
                .thenReturn(new StoreProductPageView(storeView(storeId), null));

        var mvc = MockMvcBuilders.standaloneSetup(new ProductPageController(storeViews)).build();

        mvc.perform(get("/my-store/product/" + productId))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("selectedProduct", (Object) null));
    }
}
