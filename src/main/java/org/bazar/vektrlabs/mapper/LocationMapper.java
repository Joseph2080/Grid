package org.bazar.vektrlabs.mapper;
import org.bazar.vektrlabs.dto.request.LocationRequestDto;
import org.bazar.vektrlabs.dto.response.LocationResponseDto;
import org.bazar.vektrlabs.entity.Location;
import org.jericho.common.mapper.DtoMapper;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper implements DtoMapper<Location, LocationRequestDto, LocationResponseDto> {
    @Override
    public Location convertDtoToEntity(LocationRequestDto locationRequestDto) {
        Location location = new Location();
        updateEntityFromDto(locationRequestDto, location);
        return location;
    }

    @Override
    public LocationResponseDto convertEntityToResponseDto(Location location) {
        return LocationResponseDto.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .city(location.getCity())
                .country(location.getCountry())
                .postalCode(location.getPostalCode())
                .phone(location.getPhone())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .visible(location.isVisible())
                .storeId(location.getStore().getId())
                .primary(location.isPrimary())
                .build();
    }

    @Override
    public void updateEntityFromDto(LocationRequestDto locationRequestDto, Location location) {
        location.setName(locationRequestDto.name());
        location.setAddress(locationRequestDto.address());
        location.setCity(locationRequestDto.city());
        location.setCountry(locationRequestDto.country());
        location.setPostalCode(locationRequestDto.postalCode());
        location.setPhone(locationRequestDto.phone());
        location.setLatitude(locationRequestDto.latitude());
        location.setLongitude(locationRequestDto.longitude());
        location.setVisible(locationRequestDto.visible());
    }
}