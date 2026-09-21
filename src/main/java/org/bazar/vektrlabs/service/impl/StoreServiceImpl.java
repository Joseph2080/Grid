package org.bazar.vektrlabs.service.impl;

import org.bazar.vektrlabs.dto.request.StoreRequestDto;
import org.bazar.vektrlabs.dto.response.StoreResponseDto;
import org.bazar.vektrlabs.entity.Store;
import org.bazar.vektrlabs.exception.StoreNotFoundException;
import org.bazar.vektrlabs.mapper.StoreMapper;
import org.bazar.vektrlabs.repository.StoreRepository;
import org.bazar.vektrlabs.service.StoreService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.exception.InvalidParameterException;
import org.jericho.common.service.AbstractJpaService;
import org.jericho.mediaresource.dto.MediaResourceRequestDto;
import org.jericho.mediaresource.dto.MediaResourceResponseDto;
import org.jericho.mediaresource.service.MediaResourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class StoreServiceImpl extends AbstractJpaService<
        Store,
        UUID,
        StoreRequestDto,
        StoreResponseDto,
        StoreRepository>
        implements StoreService {

    @Value("${aws.s3.bucket}")
    private String bucketKey;
    private final MediaResourceService mediaResourceService;
    private final UserProfileService userProfileService;
    private final CurrentUserUtil currentUserUtil;

    public StoreServiceImpl(
            StoreRepository repository,
            StoreMapper dtoMapper,
            MediaResourceService mediaResourceService,
            UserProfileService userProfileService,
            CurrentUserUtil currentUserUtil) {
        super(repository, dtoMapper);
        this.mediaResourceService = mediaResourceService;
        this.userProfileService = userProfileService;
        this.currentUserUtil = currentUserUtil;
    }

    @Override
    @Transactional
    public StoreResponseDto create(StoreRequestDto requestDTO) {
        var storeResponse = super.create(requestDTO);
        if (requestDTO.getLogo().isEmpty()) {
            return storeResponse;
        }
        var mediaResourceResponse = setMediaResource(storeResponse.getId(), requestDTO.getLogo());
        var store = repository.getReferenceById(storeResponse.getId());
        store.setLogo(
                mediaResourceService.findReferenceById(mediaResourceResponse.getId())
        );
        repository.save(store);
        var storeResponseDto = dtoMapper.convertEntityToResponseDto(store);
        storeResponseDto.setLogoUrl(mediaResourceService.generatePreSignedUrlForResource(mediaResourceResponse.getId()));
        return storeResponseDto;
    }

    @Override
    protected void setEntityDependencies(Store store, StoreRequestDto requestDto) {
        var profile = userProfileService.requireCompleteProfileByCognitoSub(currentUserUtil.currentCognitoSub());
        if (store.getUserProfile() != null && !store.getUserProfile().getId().equals(profile.getId())) {
            throw new AccessDeniedException("This store belongs to another profile.");
        }
        store.setUserProfile(profile);
    }

    @Override
    @Transactional
    public StoreResponseDto update(UUID id, StoreRequestDto requestDto) {
        requireOwnStore(id);
        return super.update(id, requestDto);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        requireOwnStore(id);
        super.deleteById(id);
    }

    private void requireOwnStore(UUID id) {
        var profile = userProfileService.requireCompleteProfileByCognitoSub(currentUserUtil.currentCognitoSub());
        var store = findEntityByIdOrElseThrowException(id);
        if (store.getUserProfile() == null || !profile.getId().equals(store.getUserProfile().getId())) {
            throw new AccessDeniedException("This store belongs to another profile.");
        }
    }

    @Override
    public void applyCustomValidation(StoreRequestDto requestDto){
         if(repository.findStoreByName(requestDto.getName()).isPresent()){
             throw new InvalidParameterException("Store name already exists");
         }
    }

    private MediaResourceResponseDto setMediaResource(
            UUID storeId,
            MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return mediaResourceService.create(
                        MediaResourceRequestDto.builder()
                                .context(bucketKey)
                                .objectKey(buildObjectKey(storeId, file))
                                .multipartFile(file)
                                .build());
    }

    private String buildObjectKey(
            UUID storeId,
            MultipartFile file) {
        var filename = file.getOriginalFilename();
        var extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf('.') + 1);
        }
        return "stores/"
                + storeId
                + "/logo/"
                + UUID.randomUUID()
                + (extension.isBlank() ? "" : "." + extension);
    }


    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new StoreNotFoundException("STORE_NOT_FOUND");
    }

    @Transactional(readOnly = true)
    public String generatePresignedUrlForLogo(UUID storeId) {
        Store store = findEntityByIdOrElseThrowException(storeId);
        if (store.getLogo() == null) {
            throw new StoreNotFoundException("STORE_LOGO_NOT_FOUND");
        }
        return mediaResourceService.generatePreSignedUrlForResource(store.getLogo().getId());
    }

    @Override
    @Cacheable(
            cacheNames = "store-by-name",
            key = "#name"
    )
    public StoreResponseDto findByStoreName(String name) {
        var store = repository.findStoreByName(name).orElseThrow(() ->   new StoreNotFoundException("Store by " + name + " can  not be found."));
        StoreResponseDto storeResponseDto =  dtoMapper.convertEntityToResponseDto(store);
        storeResponseDto.setLogoUrl(mediaResourceService.generatePreSignedUrlForResource(store.getLogo().getId()));
        return storeResponseDto;
    }
}