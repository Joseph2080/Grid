package org.bazar.vektrlabs.service.impl.product;

import org.bazar.vektrlabs.validation.AttributeSchemaValidator;
import org.bazar.vektrlabs.util.StoreAccessUtil;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.exception.InvalidParameterException;
import org.jericho.common.service.AbstractJpaService;
import org.bazar.vektrlabs.dto.request.ProductVariantRequestDto;
import org.bazar.vektrlabs.dto.response.ProductVariantResponseDto;
import org.bazar.vektrlabs.entity.ProductVariant;
import org.bazar.vektrlabs.exception.ProductVariantNotFoundException;
import org.bazar.vektrlabs.mapper.ProductVariantMapper;
import org.bazar.vektrlabs.repository.ProductVariantRepository;
import org.bazar.vektrlabs.service.ProductService;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductVariantServiceImpl extends AbstractJpaService<
        ProductVariant,
        UUID,
        ProductVariantRequestDto,
        ProductVariantResponseDto,
        ProductVariantRepository>
        implements ProductVariantService {

    private final ProductService productService;
    private final AttributeSchemaValidator schemaValidator;
    private final StoreAccessUtil storeAccessUtil;

    public ProductVariantServiceImpl(
            ProductVariantRepository repository,
            ProductVariantMapper dtoMapper,
            ProductService productService,
            AttributeSchemaValidator schemaValidator,
            StoreAccessUtil storeAccessUtil) {
        super(repository, dtoMapper);
        this.productService = productService;
        this.schemaValidator = schemaValidator;
        this.storeAccessUtil = storeAccessUtil;
    }

    @Override
    protected void applyCustomValidation(ProductVariantRequestDto requestDto){
        storeAccessUtil.requireAccess(productService
                .findEntityByIdOrElseThrowException(requestDto.getProductId()).getStore());
        // gpt code
        repository.findByProductId(requestDto.getProductId())
                .orElse(List.of())
                .stream()
                .filter(variant -> variant.getAttributes().equals(requestDto.getAttributes()))
                .findFirst()
                .ifPresent(existingVariant -> {
                    throw new InvalidParameterException("A product variant with the same attributes already exists for this product.");
                });
    }

    @Transactional
    @Override
    public ProductVariantResponseDto update(UUID id, ProductVariantRequestDto requestDto) {
        storeAccessUtil.requireAccess(findEntityByIdOrElseThrowException(id).getProduct().getStore());
        return super.update(id, requestDto);
    }

    @Override
    protected void deleteExternalDependencies(ProductVariant variant) {
        storeAccessUtil.requireAccess(variant.getProduct().getStore());
    }

    @Override
    public void deleteAll() {
        throw new AccessDeniedException("Unscoped product variant deletion is not permitted.");
    }

    @Override
    protected void setEntityDependencies(
            ProductVariant entity,
            ProductVariantRequestDto dto) {
        var product =   productService.findEntityByIdOrElseThrowException(dto.getProductId());
        storeAccessUtil.requireAccess(product.getStore());
        entity.setProduct(product);
        schemaValidator.validate(product.getAttributeSchema().getSchemaJson(), dto.getAttributes());
    }

    @Transactional
    @Override
    public void updateVariantStock(UUID productId, int quantity) {
        logger.info("[{}.updateProductStock] Updating stock for product variant Id={} by quantity={}", getClass().getSimpleName(), productId, quantity);
        var productVariant = findEntityByIdOrElseThrowException(productId);
        var updatedStock = productVariant.getStockQuantity() - quantity;
        productVariant.setStockQuantity(updatedStock);
        repository.save(productVariant);
        logger.debug("[{}.updateProductStock] Stock updated, new stock={}", getClass().getSimpleName(), updatedStock);
    }

    @Transactional(readOnly = true)
    @Override
    public ProductVariant   findAvailableVariant(UUID productId, int requestedQuantity) {
        logger.info("[{}.isProductAvailable] Checking availability for product variant Id={} requestedQty={}", getClass().getSimpleName(), productId, requestedQuantity);
        var response = findEntityByIdOrElseThrowException(productId);
        boolean available = response.getStockQuantity() >= requestedQuantity;
        logger.debug("[{}.isProductAvailable] Availability for productId={} is {}", getClass().getSimpleName(), productId, available);
        return available ? response : null;
    }

    @Override
    public List<ProductVariantResponseDto> findByProductId(UUID productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Product variants not found for productId: " + productId))
                .stream()
                .map(dtoMapper::convertEntityToResponseDto).toList();
    }

    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new ProductVariantNotFoundException(
                "Product variant can not be found"
        );
    }
}