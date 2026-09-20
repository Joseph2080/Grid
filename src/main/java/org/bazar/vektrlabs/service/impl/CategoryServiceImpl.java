package org.bazar.vektrlabs.service.impl;

import org.bazar.vektrlabs.dto.request.CategoryRequestDto;
import org.bazar.vektrlabs.dto.response.CategoryResponseDto;
import org.bazar.vektrlabs.entity.Category;
import org.bazar.vektrlabs.exception.CategoryNotFoundException;
import org.bazar.vektrlabs.repository.CategoryRepository;
import org.bazar.vektrlabs.service.CategoryService;
import org.bazar.vektrlabs.service.StoreService;
import org.bazar.vektrlabs.util.StoreAccessUtil;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.mapper.DtoMapper;
import org.jericho.common.service.AbstractJpaService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryServiceImpl extends AbstractJpaService<
        Category,
        UUID,
        CategoryRequestDto,
        CategoryResponseDto,
        CategoryRepository>
        implements CategoryService {

    private final StoreService storeService;
    private final StoreAccessUtil storeAccessUtil;

    public  CategoryServiceImpl(CategoryRepository repository,
            DtoMapper<Category, CategoryRequestDto, CategoryResponseDto> dtoMapper,
            StoreService storeService,
            StoreAccessUtil storeAccessUtil) {
        super(repository, dtoMapper);
        this.storeService = storeService;
        this.storeAccessUtil = storeAccessUtil;
    }

    @Override
    protected void applyCustomValidation(CategoryRequestDto requestDto) {
        storeAccessUtil.requireAccess(storeService.findEntityByIdOrElseThrowException(requestDto.storeId()));
    }

    @Transactional
    @Override
    public CategoryResponseDto update(UUID id, CategoryRequestDto requestDto) {
        storeAccessUtil.requireAccess(findEntityByIdOrElseThrowException(id).getStore());
        return super.update(id, requestDto);
    }

    @Override
    protected void deleteExternalDependencies(Category category) {
        storeAccessUtil.requireAccess(category.getStore());
    }

    @Override
    public void deleteAll() {
        throw new AccessDeniedException("Unscoped category deletion is not permitted.");
    }

    @Override
    protected void setEntityDependencies(Category entity, CategoryRequestDto requestDTO) {
        var store = storeService.findEntityByIdOrElseThrowException(requestDTO.storeId());
        storeAccessUtil.requireAccess(store);
        entity.setStore(store);
    }

    public List<CategoryResponseDto> findByStoreId(UUID storeId) {
        var categories = repository.findByStoreId(storeId).orElseThrow(() -> new CategoryNotFoundException("Categories not found for store with id: " + storeId));
        return categories.stream()
                .map(dtoMapper::convertEntityToResponseDto)
                .toList();
    }

    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new CategoryNotFoundException("Category not found");
    }
}