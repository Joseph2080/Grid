package org.bazar.vektrlabs.controller.page;

import lombok.AllArgsConstructor;
import org.bazar.vektrlabs.dto.response.StoreViewResponse;
import org.bazar.vektrlabs.facade.store.StoreViewFacade;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class StorePageController {

    private final StoreViewFacade storeViewFacade;

    @GetMapping("/{slug:[^\\.]+}")
    public String store(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {
        StoreViewResponse store = storeViewFacade.getViewByNameCategory(slug, page, size);
        model.addAttribute("store", store);
        model.addAttribute("storeSlug", slug);

        return "index";
    }
}