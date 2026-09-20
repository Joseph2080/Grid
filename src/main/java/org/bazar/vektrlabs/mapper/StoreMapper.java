package org.bazar.vektrlabs.mapper;

import org.bazar.vektrlabs.dto.request.StoreRequestDto;
import org.bazar.vektrlabs.dto.response.StoreResponseDto;
import org.bazar.vektrlabs.entity.Store;
import org.jericho.common.mapper.DtoMapper;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper implements DtoMapper<Store, StoreRequestDto, StoreResponseDto> {

    @Override
    public Store convertDtoToEntity(StoreRequestDto request) {
        if (request == null) {
            return null;
        }
        Store store = new Store();
        updateEntityFromDto(request, store);
        return store;
    }

    @Override
    public StoreResponseDto convertEntityToResponseDto(Store entity) {
        if (entity == null) {
            return null;
        }

        return StoreResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .facebookUrl(entity.getFacebookUrl())
                .instagramUrl(entity.getInstagramUrl())
                .xUrl(entity.getXUrl())
                .tiktokUrl(entity.getTiktokUrl())
                .youtubeUrl(entity.getYoutubeUrl())
                .linkedinUrl(entity.getLinkedinUrl())
                .currency(entity.getCurrency())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                // TODO: Configure mediaResource (logoUrl / logoAltText) mapping later
                .build();
    }

    @Override
    public void updateEntityFromDto(StoreRequestDto request, Store entity) {
        if (request == null || entity == null) {
            return;
        }
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setFacebookUrl(request.getFacebookUrl());
        entity.setInstagramUrl(request.getInstagramUrl());
        entity.setXUrl(request.getTwitterUrl());
        entity.setTiktokUrl(request.getTiktokUrl());
        entity.setYoutubeUrl(request.getYoutubeUrl());
        entity.setLinkedinUrl(request.getLinkedinUrl());
        entity.setCurrency(request.getCurrency());
        // TODO: Configure mediaResource (logo) update later
    }
}