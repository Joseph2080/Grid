package org.bazar.vektrlabs.controller.page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bazar.vektrlabs.dto.response.ProductMediaResourceResponseDto;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.facade.store.StoreViewFacade;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * Renders a single product on top of the storefront, at a stable, shareable
 * URL: /{slug}/product/{productId}. Reuses the same "index" template as
 * StorePageController so the product opens over the full storefront (nav,
 * cart, chat), but overrides the page's OG/meta tags with the product's own
 * details for link previews (social shares, QR codes, etc.).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductPageController {

    private final StoreViewFacade storeViewFacade;

    @GetMapping("/{slug:[^\\.]+}/product/{productId}")
    public String product(
            @PathVariable String slug,
            @PathVariable UUID productId,
            Model model) {
        var result = storeViewFacade.getViewWithSelectedProduct(slug, productId);
        ProductResponseDto selectedProduct = result.selectedProduct();

        if (selectedProduct == null) {
            log.warn("Shared product {} not found in store '{}'; rendering plain store view.", productId, slug);
        }

        model.addAttribute("store", result.storeView());
        model.addAttribute("storeSlug", slug);
        model.addAttribute("selectedProduct", selectedProduct);
        model.addAttribute("selectedProductImage", resolvePrimaryImage(selectedProduct));

        return "index";
    }

    // The repository already returns media primary-first, then sort order
    // (ProductMediaResourceRepository#findByProductIdIn), so the first entry
    // is always the correct display image - no re-sorting needed here.
    private String resolvePrimaryImage(ProductResponseDto product) {
        if (product == null) {
            return null;
        }
        List<ProductMediaResourceResponseDto> media = product.getProductMediaResources();
        if (media == null || media.isEmpty()) {
            return null;
        }
        return media.get(0).getPreSignedUrl();
    }
}
