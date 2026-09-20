package org.bazar.vektrlabs.service.impl.product;

import org.bazar.vektrlabs.entity.Category;
import org.bazar.vektrlabs.entity.Store;
import org.bazar.vektrlabs.service.AttributeSchemaService;
import org.bazar.vektrlabs.service.CategoryService;
import org.bazar.vektrlabs.service.StoreService;
import org.bazar.vektrlabs.util.StoreAccessUtil;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.service.AbstractJpaService;
import org.bazar.vektrlabs.dto.request.ProductRequestDto;
import org.bazar.vektrlabs.dto.response.ProductResponseDto;
import org.bazar.vektrlabs.entity.Product;
import org.bazar.vektrlabs.exception.ProductNotFoundException;
import org.bazar.vektrlabs.mapper.ProductMapper;
import org.bazar.vektrlabs.repository.ProductRepository;
import org.bazar.vektrlabs.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl extends AbstractJpaService<
        Product,
        UUID,
        ProductRequestDto,
        ProductResponseDto,
        ProductRepository>
        implements ProductService {

    private final StoreService storeService;
    private final CategoryService categoryService;
    private final AttributeSchemaService attributeSchemaService;
    private final StoreAccessUtil storeAccessUtil;

    public ProductServiceImpl(
            ProductRepository repository,
            ProductMapper dtoMapper,
            StoreService storeService,
            CategoryService categoryService,
            AttributeSchemaService attributeSchemaService,
            StoreAccessUtil storeAccessUtil) {
        super(repository, dtoMapper);
        this.storeService = storeService;
        this.categoryService = categoryService;
        this.attributeSchemaService = attributeSchemaService;
        this.storeAccessUtil = storeAccessUtil;
    }

    @Override
    protected void applyCustomValidation(ProductRequestDto requestDto) {
        var store = storeService.findEntityByIdOrElseThrowException(requestDto.storeId());
        storeAccessUtil.requireAccess(store);
        requireCategoryInStore(requestDto.categoryId(), store);
    }

    @Transactional
    @Override
    public ProductResponseDto update(UUID id, ProductRequestDto requestDto) {
        storeAccessUtil.requireAccess(findEntityByIdOrElseThrowException(id).getStore());
        return super.update(id, requestDto);
    }

    @Override
    protected void deleteExternalDependencies(Product product) {
        storeAccessUtil.requireAccess(product.getStore());
    }

    @Override
    public void deleteAll() {
        throw new AccessDeniedException("Unscoped product deletion is not permitted.");
    }

    private Category requireCategoryInStore(UUID categoryId, Store store) {
        var category = categoryService.findEntityByIdOrElseThrowException(categoryId);
        if (category.getStore() == null || !store.getId().equals(category.getStore().getId())) {
            throw new AccessDeniedException("The product category must belong to the same store.");
        }
        return category;
    }

    @Override
    public void setEntityDependencies(Product product, ProductRequestDto productRequestDto){
        logger.info("Validating product request DTO: {}", productRequestDto);
        if (product.getStore() != null) {
            storeAccessUtil.requireAccess(product.getStore());
        }
        var store = storeService.findEntityByIdOrElseThrowException(productRequestDto.storeId());
        storeAccessUtil.requireAccess(store);
        var category = requireCategoryInStore(productRequestDto.categoryId(), store);
        product.setStore(store);
        logger.info("Store found for and assigned to product");
        product.setCategory(category);
        product.setAttributeSchema(attributeSchemaService.findEntityByCode(productRequestDto.attributeSchemaCode()));
    }

    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new ProductNotFoundException("PRODUCT_NOT_FOUND");
    }

    public List<ProductResponseDto> findAllByStoreId(UUID storeId){
        var products = repository.findByStoreId(storeId).orElseThrow(() -> new ProductNotFoundException("Product not found for store " + storeId));
        return products.stream()
                .map(dtoMapper::convertEntityToResponseDto)
                .toList();
    }

    @Override
    public List<ProductResponseDto> findByCategoryId(UUID categoryId) {
        return repository.findByCategoryId(categoryId)
                .orElse(List.of())
                .stream()
                .map(dtoMapper::convertEntityToResponseDto)
                .toList();
    }
}