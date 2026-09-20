package org.bazar.vektrlabs.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bazar.vektrlabs.dto.response.ProductMediaResourceResponseDto;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.service.ProductMediaResourceService;
import org.bazar.vektrlabs.service.ProductService;
import org.bazar.vektrlabs.util.ProductAttributeResourceBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductResourceFacade {
    // investigate how this can be optimized as well!
    private final ProductService productService;
    private final ProductMediaResourceService productMediaResourceService;
    private final ProductAttributeResourceBuilder productAttributeResourceBuilder;

    @Cacheable(
            cacheNames = "product-by-id",
            key = "#id"
    )
    public ProductResponseDto findById(UUID id) {
        log.debug("Fetching product by id: {}", id);
        ProductResponseDto productResponseDto = productService.findByIdOrElseThrowException(id);
        productResponseDto.setAttributes(
                productAttributeResourceBuilder.buildAttributes(productResponseDto.getVariants())
        );
        productResponseDto.setProductMediaResources(
                productMediaResourceService.findByProductId(productResponseDto.getId()));
        log.debug("Product fetched successfully. id={}, mediaCount={}",
                id,
                productResponseDto.getProductMediaResources().size());
        return productResponseDto;
    }

    @Cacheable(
            cacheNames = "products-by-category",
            key = "#categoryId"
    )
    public List<ProductResponseDto> findByCategoryId(UUID categoryId) {
        log.debug("Fetching products by category id: {}", categoryId);
        List<ProductResponseDto> productResponseDto = productService.findByCategoryId(categoryId);
        log.debug("Found {} products for category id: {}", productResponseDto.size(), categoryId);
        return productResponseDto.stream().map(productResponseDto1 -> {
            productResponseDto1.setAttributes(
                    productAttributeResourceBuilder.buildAttributes(productResponseDto1.getVariants())
            );
            productResponseDto1.setProductMediaResources(
                    productMediaResourceService.findByProductId(productResponseDto1.getId()));
            return productResponseDto1;
        }).toList();
    }

    @Cacheable(
            cacheNames = "products-page",
            key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
    )
    public List<ProductResponseDto> findAll(Pageable pageable) {
        log.debug("Fetching products. page={}, size={}, sort={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());
        List<ProductResponseDto > products =
                productService.findAll(pageable);
        if (products.isEmpty()) {
            log.debug("No products found. page={}, size={}",
                    pageable.getPageNumber(),
                    pageable.getPageSize());
            return products;
        }
        log.debug("Found {} products. page={}, size={}",
                products.size(),
                pageable.getPageNumber(),
                pageable.getPageSize());
        return products.stream()
                .map(productResponseDto -> {
                    productResponseDto.setAttributes(
                            productAttributeResourceBuilder.buildAttributes(productResponseDto.getVariants())
                    );
                    productResponseDto
                            .setProductMediaResources(productMediaResourceService
                                    .findByProductId(productResponseDto.getId()));
                    return productResponseDto;
                }).toList();
    }

    @Cacheable(
            cacheNames = "products-by-store",
            key = "#storeId"
    )
    public List<ProductResponseDto> findAllByStoreId(UUID storeId) {
        log.debug("Fetching products by store id: {}", storeId);
        List<ProductResponseDto> products = productService.findAllByStoreId(storeId);
        log.debug("Found {} products for store id: {}", products.size(), storeId);
        List<UUID> productIds = products.stream()
                .map(ProductResponseDto::getId)
                .toList();
        if (productIds.isEmpty()) {
            log.debug("No products found for store id: {}", storeId);
            return products;
        }
        log.debug("Fetching media resources for {} products in store id: {}",
                productIds.size(),
                storeId);
        Map<UUID, List<ProductMediaResourceResponseDto>> mediaByProductId =
                productMediaResourceService.findByProductIds(productIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                ProductMediaResourceResponseDto::getProductId
                        ));
        products.forEach(product ->
                product.setAttributes(
                        productAttributeResourceBuilder.buildAttributes(product.getVariants())
                ));
        products.forEach(product ->
                product.setProductMediaResources(
                        mediaByProductId.getOrDefault(product.getId(), List.of())
                )
        );
        log.debug("Products fetched successfully for store id: {}. productCount={}, mediaGroupCount={}",
                storeId,
                products.size(),
                mediaByProductId.size());
        return products;
    }
}