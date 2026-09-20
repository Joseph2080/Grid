package org.bazar.vektrlabs.service;

import org.bazar.vektrlabs.dto.request.StoreRequestDto;
import org.bazar.vektrlabs.dto.response.StoreResponseDto;
import org.bazar.vektrlabs.entity.Store;
import org.jericho.common.service.Service;

import java.util.UUID;

public interface StoreService extends Service<
        Store,
        UUID,
        StoreRequestDto,
        StoreResponseDto> {
    StoreResponseDto findByStoreName(String name);
}