package org.bazar.vektrlabs.service.impl;

import org.bazar.vektrlabs.dto.request.CategoryRequestDto;
import org.bazar.vektrlabs.dto.request.LocationRequestDto;
import org.bazar.vektrlabs.entity.ProductVariant;
import org.bazar.vektrlabs.entity.Store;
import org.bazar.vektrlabs.entity.UserProfile;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.mapper.CategoryMapper;
import org.bazar.vektrlabs.mapper.LocationMapper;
import org.bazar.vektrlabs.mapper.ProductMapper;
import org.bazar.vektrlabs.mapper.ProductMediaResourceMapper;
import org.bazar.vektrlabs.mapper.ProductVariantMapper;
import org.bazar.vektrlabs.mapper.StoreMapper;
import org.bazar.vektrlabs.mapper.UserProfileMapper;
import org.bazar.vektrlabs.repository.CategoryRepository;
import org.bazar.vektrlabs.repository.LocationRepository;
import org.bazar.vektrlabs.repository.ProductMediaResourceRepository;
import org.bazar.vektrlabs.repository.ProductRepository;
import org.bazar.vektrlabs.repository.ProductVariantRepository;
import org.bazar.vektrlabs.repository.StoreRepository;
import org.bazar.vektrlabs.repository.UserProfileRepository;
import org.bazar.vektrlabs.service.AttributeSchemaService;
import org.bazar.vektrlabs.service.CategoryService;
import org.bazar.vektrlabs.service.LocationService;
import org.bazar.vektrlabs.service.ProductMediaResourceService;
import org.bazar.vektrlabs.service.ProductService;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.bazar.vektrlabs.service.StoreService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.service.impl.product.ProductMediaResourceServiceImpl;
import org.bazar.vektrlabs.service.impl.product.ProductServiceImpl;
import org.bazar.vektrlabs.service.impl.product.ProductVariantServiceImpl;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.bazar.vektrlabs.util.StoreAccessUtil;
import org.bazar.vektrlabs.validation.AttributeSchemaValidator;
import org.jericho.mediaresource.service.MediaResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StoreResourceWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ResourceServicesConfiguration.class)
            .withPropertyValues("aws.s3.bucket=unit-test-bucket")
            .withBean(CurrentUserUtil.class, () -> mock(CurrentUserUtil.class))
            .withBean(UserProfileRepository.class, () -> mock(UserProfileRepository.class))
            .withBean(StoreRepository.class, () -> mock(StoreRepository.class))
            .withBean(ProductRepository.class, () -> mock(ProductRepository.class))
            .withBean(ProductVariantRepository.class, () -> mock(ProductVariantRepository.class))
            .withBean(ProductMediaResourceRepository.class, () -> mock(ProductMediaResourceRepository.class))
            .withBean(CategoryRepository.class, () -> mock(CategoryRepository.class))
            .withBean(LocationRepository.class, () -> mock(LocationRepository.class))
            .withBean(AttributeSchemaService.class, () -> mock(AttributeSchemaService.class))
            .withBean(AttributeSchemaValidator.class, () -> mock(AttributeSchemaValidator.class))
            .withBean(MediaResourceService.class, () -> mock(MediaResourceService.class))
            .withBean(PlatformTransactionManager.class, () -> {
                var transactions = mock(PlatformTransactionManager.class);
                when(transactions.getTransaction(any())).thenAnswer(ignored -> new SimpleTransactionStatus());
                return transactions;
            });

    @Test
    void resourceStoreAndProfileServicesWireWithoutCircularDependencies() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(StoreAccessUtil.class);
            for (var serviceType : List.of(ProductService.class, ProductVariantService.class,
                    ProductMediaResourceService.class, CategoryService.class, LocationService.class,
                    StoreService.class, UserProfileService.class)) {
                assertThat(context).hasSingleBean(serviceType);
                assertThat(AopUtils.isAopProxy(context.getBean(serviceType))).isTrue();
            }
        });
    }

    @Test
    void proxiedCreateResolvesStoreAndCompleteProfileThroughRealServices() {
        contextRunner.run(context -> {
            var profile = profile();
            var store = store(profile);
            var repository = context.getBean(CategoryRepository.class);
            when(context.getBean(CurrentUserUtil.class).currentCognitoSub()).thenReturn("owner");
            when(context.getBean(UserProfileRepository.class).findByCognitoSub("owner"))
                    .thenReturn(Optional.of(profile));
            when(context.getBean(StoreRepository.class).findById(store.getId())).thenReturn(Optional.of(store));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var response = context.getBean(CategoryService.class)
                    .create(new CategoryRequestDto("Owned category", store.getId()));

            assertThat(response.getStoreId()).isEqualTo(store.getId());
            verify(repository).save(any());
            verifyNoInteractions(context.getBean(MediaResourceService.class));
        });
    }

    @Test
    void proxiedCreateUsesRealProfileCompletenessGuardBeforeSideEffects() {
        contextRunner.run(context -> {
            var profile = profile();
            profile.setLastName(" ");
            var store = store(profile);
            var repository = context.getBean(CategoryRepository.class);
            when(context.getBean(CurrentUserUtil.class).currentCognitoSub()).thenReturn("owner");
            when(context.getBean(UserProfileRepository.class).findByCognitoSub("owner"))
                    .thenReturn(Optional.of(profile));
            when(context.getBean(StoreRepository.class).findById(store.getId())).thenReturn(Optional.of(store));

            assertThrows(ProfileIncompleteException.class, () -> context.getBean(CategoryService.class)
                    .create(new CategoryRequestDto("Rejected category", store.getId())));

            verify(repository, never()).save(any());
            verifyNoInteractions(context.getBean(MediaResourceService.class));
        });
    }

    @Test
    void proxiedCreateRejectsForeignStoreBeforeClearingPrimaryLocation() {
        contextRunner.run(context -> {
            var profile = profile();
            var foreignStore = store(profile());
            var repository = context.getBean(LocationRepository.class);
            when(context.getBean(CurrentUserUtil.class).currentCognitoSub()).thenReturn("owner");
            when(context.getBean(UserProfileRepository.class).findByCognitoSub("owner"))
                    .thenReturn(Optional.of(profile));
            when(context.getBean(StoreRepository.class).findById(foreignStore.getId()))
                    .thenReturn(Optional.of(foreignStore));

            assertThrows(AccessDeniedException.class, () -> context.getBean(LocationService.class)
                    .create(LocationRequestDto.builder().name("Foreign location").address("Address")
                            .storeId(foreignStore.getId()).primary(true).build()));

            verify(repository, never()).unsetPrimaryByStoreId(any());
            verify(repository, never()).save(any());
        });
    }

    @Test
    void proxiedSystemStockUpdateDoesNotResolveAnInteractiveProfile() {
        contextRunner.run(context -> {
            var variant = new ProductVariant();
            variant.setId(UUID.randomUUID());
            variant.setStockQuantity(7);
            var repository = context.getBean(ProductVariantRepository.class);
            when(repository.findById(variant.getId())).thenReturn(Optional.of(variant));

            context.getBean(ProductVariantService.class).updateVariantStock(variant.getId(), 2);

            assertThat(variant.getStockQuantity()).isEqualTo(5);
            verify(repository).save(variant);
            verifyNoInteractions(context.getBean(CurrentUserUtil.class), context.getBean(UserProfileRepository.class));
        });
    }

    private static UserProfile profile() {
        var profile = new UserProfile();
        profile.setId(UUID.randomUUID());
        profile.setFirstName("First");
        profile.setLastName("Last");
        return profile;
    }

    private static Store store(UserProfile profile) {
        var store = new Store();
        store.setId(UUID.randomUUID());
        store.setUserProfile(profile);
        return store;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import({StoreAccessUtil.class, ProductServiceImpl.class, ProductVariantServiceImpl.class,
            ProductMediaResourceServiceImpl.class, CategoryServiceImpl.class, LocationServiceImpl.class,
            StoreServiceImpl.class, UserProfileServiceImpl.class, ProductMapper.class,
            ProductVariantMapper.class, ProductMediaResourceMapper.class, CategoryMapper.class,
            LocationMapper.class, StoreMapper.class, UserProfileMapper.class})
    static class ResourceServicesConfiguration {
    }
}
