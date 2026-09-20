package org.bazar.vektrlabs.service.impl;

import org.bazar.vektrlabs.dto.request.LocationRequestDto;
import org.bazar.vektrlabs.dto.response.LocationResponseDto;
import org.bazar.vektrlabs.entity.Location;
import org.bazar.vektrlabs.exception.LocationNotFoundException;
import org.bazar.vektrlabs.mapper.LocationMapper;
import org.bazar.vektrlabs.repository.LocationRepository;
import org.bazar.vektrlabs.service.LocationService;
import org.bazar.vektrlabs.service.StoreService;
import org.bazar.vektrlabs.util.StoreAccessUtil;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.service.AbstractJpaService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LocationServiceImpl extends AbstractJpaService<
        Location,
        UUID,
        LocationRequestDto,
        LocationResponseDto,
        LocationRepository>
        implements LocationService {

    private final StoreService storeService;
    private final StoreAccessUtil storeAccessUtil;

    protected LocationServiceImpl(
            LocationRepository repository,
            LocationMapper dtoMapper,
            StoreService storeService,
            StoreAccessUtil storeAccessUtil) {
        super(repository, dtoMapper);
        this.storeService = storeService;
        this.storeAccessUtil = storeAccessUtil;
    }

    @Override
    protected void applyCustomValidation(LocationRequestDto requestDto) {
        storeAccessUtil.requireAccess(storeService.findEntityByIdOrElseThrowException(requestDto.storeId()));
    }

    @Transactional
    @Override
    public LocationResponseDto update(UUID id, LocationRequestDto requestDto) {
        storeAccessUtil.requireAccess(findEntityByIdOrElseThrowException(id).getStore());
        return super.update(id, requestDto);
    }

    @Override
    protected void deleteExternalDependencies(Location location) {
        storeAccessUtil.requireAccess(location.getStore());
    }

    @Override
    public void deleteAll() {
        throw new AccessDeniedException("Unscoped location deletion is not permitted.");
    }

    @Override
    protected void setEntityDependencies(
            Location location,
            LocationRequestDto requestDTO) {
        var store = storeService
                .findEntityByIdOrElseThrowException(requestDTO.storeId());
        storeAccessUtil.requireAccess(store);
        location.setStore(store);
        setPrimaryLocation(location, requestDTO.storeId(), requestDTO.primary());
    }

    private void setPrimaryLocation(
            Location entity,
            UUID storeId,
            Boolean primaryImage) {
        if (!Boolean.TRUE.equals(primaryImage)) {
            return;
        }
        int result = repository.unsetPrimaryByStoreId(storeId);
        if (result != 1) {
            logger.warn("Warning: No primary location was cleared for storeId: {}", storeId);
        }
        entity.setPrimary(true);
    }

    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new LocationNotFoundException("Location not found sir");
    }

    @Override
    public List<LocationResponseDto> findAllByStoreId(UUID storeId) {
        return repository.findAllByStoreId(storeId)
                .orElseThrow(() -> new LocationNotFoundException("Location not found for storeId: " + storeId))
                .stream()
                .map(dtoMapper::convertEntityToResponseDto)
                .toList();
    }
}
