package org.bazar.vektrlabs.service.impl.product;

import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.service.AbstractJpaService;
import org.bazar.vektrlabs.dto.request.ProductMediaResourceRequestDto;
import org.bazar.vektrlabs.dto.response.ProductMediaResourceResponseDto;
import org.bazar.vektrlabs.entity.ProductMediaResource;
import org.bazar.vektrlabs.exception.ProductMediaResourceNotFoundException;
import org.bazar.vektrlabs.mapper.ProductMediaResourceMapper;
import org.bazar.vektrlabs.repository.ProductMediaResourceRepository;
import org.bazar.vektrlabs.service.ProductMediaResourceService;
import org.bazar.vektrlabs.service.ProductService;
import org.bazar.vektrlabs.util.StoreAccessUtil;
import org.jericho.mediaresource.dto.MediaResourceRequestDto;
import org.jericho.mediaresource.dto.MediaResourceResponseDto;
import org.jericho.mediaresource.service.MediaResourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class ProductMediaResourceServiceImpl extends AbstractJpaService<
        ProductMediaResource,
        UUID,
        ProductMediaResourceRequestDto,
        ProductMediaResourceResponseDto,
        ProductMediaResourceRepository>
        implements ProductMediaResourceService {

    private final ProductService productService;
    private final MediaResourceService mediaResourceService;
    private final StoreAccessUtil storeAccessUtil;

    @Value("${aws.s3.bucket}")
    private String bucketKey;

    public ProductMediaResourceServiceImpl(
            ProductMediaResourceRepository repository,
            ProductMediaResourceMapper dtoMapper,
            ProductService productService,
            MediaResourceService mediaResourceService,
            StoreAccessUtil storeAccessUtil) {
        super(repository, dtoMapper);
        this.productService = productService;
        this.mediaResourceService = mediaResourceService;
        this.storeAccessUtil = storeAccessUtil;
    }

    @Override
    protected void applyCustomValidation(ProductMediaResourceRequestDto requestDto) {
        storeAccessUtil.requireAccess(productService
                .findEntityByIdOrElseThrowException(requestDto.getProductId()).getStore());
    }

    @Override
    protected void deleteExternalDependencies(ProductMediaResource mediaResource) {
        storeAccessUtil.requireAccess(mediaResource.getProduct().getStore());
    }

    @Override
    public void deleteAll() {
        throw new AccessDeniedException("Unscoped product media deletion is not permitted.");
    }

    @Override
    protected void setEntityDependencies(
            ProductMediaResource entity,
            ProductMediaResourceRequestDto dto) {

        var productId = dto.getProductId();

        setProduct(entity, productId);
        setMediaResource(entity, dto.getFile());
        setPrimaryImage(entity, productId, dto.getPrimaryImage());
        Integer nextSortOrder = repository
                .findMaxSortOrderByProductId(productId)
                .orElse(0) + 1;
        entity.setSortOrder(nextSortOrder);
    }

    private void setProduct(ProductMediaResource entity, UUID productId) {
        var product = productService.findEntityByIdOrElseThrowException(productId);
        storeAccessUtil.requireAccess(product.getStore());
        entity.setProduct(product);
    }

    @Transactional
    @Override
    public ProductMediaResourceResponseDto update(UUID id,
            ProductMediaResourceRequestDto productMediaResourceRequestDto) {
        storeAccessUtil.requireAccess(findEntityByIdOrElseThrowException(id).getProduct().getStore());
        applyCustomValidation(productMediaResourceRequestDto);
        throw new UnsupportedOperationException("You can only create and delete productMediaResources for now");
    }

    private void setMediaResource(
            ProductMediaResource entity,
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return;
        }
        MediaResourceResponseDto mediaResponse =
                mediaResourceService.create(
                        MediaResourceRequestDto.builder()
                                .context(bucketKey)
                                .objectKey(buildObjectKey(entity, file))
                                .multipartFile(file)
                                .build());
        entity.setMediaResource(
                mediaResourceService.findReferenceById(mediaResponse.getId())
        );
    }

    private void setPrimaryImage(
            ProductMediaResource entity,
            UUID productId,
            Boolean primaryImage) {
        if (!Boolean.TRUE.equals(primaryImage)) {
            return;
        }
        int result = repository.unsetPrimaryImageByProductId(productId);
        if (result != 1) {
            logger.warn("Warning: No primary image was cleared for productId: {}", productId);
        }
        entity.setPrimaryImage(true);
    }

    private String buildObjectKey(
            ProductMediaResource entity,
            MultipartFile file) {
        String filename = file.getOriginalFilename();
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf('.') + 1);
        }
        return "products/"
                + entity.getProduct().getId()
                + "/images/"
                + UUID.randomUUID()
                + (extension.isBlank() ? "" : "." + extension);
    }

    @Override
    public List<ProductMediaResourceResponseDto> findByProductId(UUID productId) {
        return repository.findByProductId(productId).stream()
                .map(this::tagMediaResourceToDto)
                .toList();
    }

    @Override
    public List<ProductMediaResourceResponseDto> findByProductIds(List<UUID> productIds) {
        return repository.findByProductIdIn(productIds).stream()
                .map(this::tagMediaResourceToDto)
                .toList();
    }

    private ProductMediaResourceResponseDto tagMediaResourceToDto(ProductMediaResource productMediaResource) {
        ProductMediaResourceResponseDto productMediaResourceResponseDto = dtoMapper.convertEntityToResponseDto(productMediaResource);
        productMediaResourceResponseDto.setPreSignedUrl(mediaResourceService.generatePreSignedUrlForResource(productMediaResource.getMediaResource().getId()));
        return productMediaResourceResponseDto;
    }

    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new ProductMediaResourceNotFoundException(
                "PRODUCT_MEDIA_RESOURCE_NOT_FOUND"
        );
    }
}
