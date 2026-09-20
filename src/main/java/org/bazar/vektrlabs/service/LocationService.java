package org.bazar.vektrlabs.service;

import org.bazar.vektrlabs.dto.request.LocationRequestDto;
import org.bazar.vektrlabs.dto.response.LocationResponseDto;
import org.bazar.vektrlabs.entity.Location;
import org.jericho.common.service.Service;

import java.util.List;
import java.util.UUID;

public interface LocationService extends Service<Location, UUID, LocationRequestDto, LocationResponseDto> {
    List<LocationResponseDto> findAllByStoreId(UUID storeId);
}
