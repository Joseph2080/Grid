package org.bazar.vektrlabs.facade.store;

import lombok.RequiredArgsConstructor;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.dto.response.StoreProductPageView;
import org.bazar.vektrlabs.dto.response.StoreResponseDto;
import org.bazar.vektrlabs.dto.response.StoreViewResponse;
import org.bazar.vektrlabs.facade.CategoryResourceFacade;
import org.bazar.vektrlabs.facade.ProductResourceFacade;
import org.bazar.vektrlabs.service.LocationService;
import org.bazar.vektrlabs.service.StoreService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreViewFacade {

    private final ProductResourceFacade productResourceFacade;
    private final StoreService storeService;
    private final CategoryResourceFacade categoryResourceFacade;
    private final LocationService locationService;

    private static final int DEFAULT_PAGE_SIZE = 12;

    // we need to create a default category for products that do not have a category assigned to them.
    // This will allow us to display all products in the store, even if they do not have a category.
    public StoreViewResponse getViewByNameCategory(String storeName) {
        return getViewByNameCategory(storeName, 1, DEFAULT_PAGE_SIZE);
    }

    public StoreViewResponse getViewByNameCategory(String storeName, int page, int pageSize) {
        var store = storeService.findByStoreName(storeName);
        var allProducts = productResourceFacade.findAllByStoreId(store.getId());
        return buildStoreView(store, allProducts, page, pageSize);
    }

    // Builds the storefront view and resolves the selected product from the
    // same already-fetched product list, rather than issuing a separate
    // product-by-id lookup (findAllByStoreId already loaded every product,
    // with media, for this store).
    // we should maybe find a way better way of loading  the product from the store as well
    public StoreProductPageView getViewWithSelectedProduct(String storeName, UUID productId) {
        var store = storeService.findByStoreName(storeName);
        var allProducts = productResourceFacade.findAllByStoreId(store.getId());
        var storeView = buildStoreView(store, allProducts, 1, DEFAULT_PAGE_SIZE);
        var selectedProduct = allProducts.stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElse(null);

        return new StoreProductPageView(storeView, selectedProduct);
    }

    private StoreViewResponse buildStoreView(
            StoreResponseDto store,
            List<ProductResponseDto> allProducts,
            int page,
            int pageSize) {
        var categories = categoryResourceFacade.findAllByStoreId(store.getId(), allProducts);
        var locations = locationService.findAllByStoreId(store.getId());

        var safePage = Math.max(page, 1);
        var safePageSize = Math.max(pageSize, 1);
        var totalProducts = allProducts.size();
        var totalPages = totalProducts == 0 ? 1 : (int) Math.ceil((double) totalProducts / safePageSize);
        var boundedPage = Math.min(safePage, totalPages);
        var offset = (boundedPage - 1) * safePageSize;
        var pagedProducts = allProducts.stream()
                .skip(offset)
                .limit(safePageSize)
                .toList();

        return new StoreViewResponse(
                store,
                categories,
                locations,
                pagedProducts,
                boundedPage,
                safePageSize,
                totalProducts,
                totalPages,
                boundedPage < totalPages,
                boundedPage > 1
        );
    }
}

