package org.bazar.vektrlabs.service.impl;

import org.bazar.vektrlabs.dto.request.CategoryRequestDto;
import org.bazar.vektrlabs.dto.request.LocationRequestDto;
import org.bazar.vektrlabs.dto.request.ProductMediaResourceRequestDto;
import org.bazar.vektrlabs.dto.request.ProductRequestDto;
import org.bazar.vektrlabs.dto.request.ProductVariantRequestDto;
import org.bazar.vektrlabs.entity.AttributeSchema;
import org.bazar.vektrlabs.entity.Category;
import org.bazar.vektrlabs.entity.Location;
import org.bazar.vektrlabs.entity.Product;
import org.bazar.vektrlabs.entity.ProductMediaResource;
import org.bazar.vektrlabs.entity.ProductVariant;
import org.bazar.vektrlabs.entity.Store;
import org.bazar.vektrlabs.entity.UserProfile;
import org.bazar.vektrlabs.exception.ProfileIncompleteException;
import org.bazar.vektrlabs.exception.ProfileNotFoundException;
import org.bazar.vektrlabs.mapper.CategoryMapper;
import org.bazar.vektrlabs.mapper.LocationMapper;
import org.bazar.vektrlabs.mapper.ProductMapper;
import org.bazar.vektrlabs.mapper.ProductMediaResourceMapper;
import org.bazar.vektrlabs.mapper.ProductVariantMapper;
import org.bazar.vektrlabs.repository.CategoryRepository;
import org.bazar.vektrlabs.repository.LocationRepository;
import org.bazar.vektrlabs.repository.ProductMediaResourceRepository;
import org.bazar.vektrlabs.repository.ProductRepository;
import org.bazar.vektrlabs.repository.ProductVariantRepository;
import org.bazar.vektrlabs.service.AttributeSchemaService;
import org.bazar.vektrlabs.service.CategoryService;
import org.bazar.vektrlabs.service.ProductService;
import org.bazar.vektrlabs.service.StoreService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.bazar.vektrlabs.service.impl.product.ProductMediaResourceServiceImpl;
import org.bazar.vektrlabs.service.impl.product.ProductServiceImpl;
import org.bazar.vektrlabs.service.impl.product.ProductVariantServiceImpl;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.bazar.vektrlabs.util.StoreAccessUtil;
import org.bazar.vektrlabs.validation.AttributeSchemaValidator;
import org.jericho.common.entity.BaseJpaEntity;
import org.jericho.common.mapper.DtoMapper;
import org.jericho.common.service.Service;
import org.jericho.mediaresource.dto.MediaResourceResponseDto;
import org.jericho.mediaresource.service.MediaResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StoreResourceAuthorizationTest {

    enum Resource { PRODUCT, VARIANT, MEDIA, CATEGORY, LOCATION }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void rejectsCreatingInAnotherUsersStoreBeforeSideEffects(Resource resource) {
        var fixture = fixture(resource);

        assertThrows(AccessDeniedException.class, () -> fixture.create(fixture.context.foreignStore));

        fixture.assertNoMutation();
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void cannotTakeOverForeignResourceByChangingItsParent(Resource resource) {
        var fixture = fixture(resource);
        fixture.assignStore.accept(fixture.context.foreignStore);

        assertThrows(AccessDeniedException.class, () -> fixture.update(fixture.context.ownStore));

        fixture.assertNoMutation();
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void cannotMoveOwnResourceToForeignParent(Resource resource) {
        var fixture = fixture(resource);

        assertThrows(AccessDeniedException.class, () -> fixture.update(fixture.context.foreignStore));

        fixture.assertNoMutation();
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void rejectsDeletingForeignResource(Resource resource) {
        var fixture = fixture(resource);
        fixture.assignStore.accept(fixture.context.foreignStore);

        assertThrows(AccessDeniedException.class, fixture::delete);

        fixture.assertNoMutation();
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void rejectsUnscopedBulkDeletion(Resource resource) {
        var fixture = fixture(resource);

        assertThrows(AccessDeniedException.class, fixture.service::deleteAll);

        verifyNoInteractions(fixture.repository, fixture.mapper);
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void allowsCreatingInOwnStore(Resource resource) {
        var fixture = fixture(resource);

        assertDoesNotThrow(() -> fixture.create(fixture.context.ownStore));

        verify(fixture.repository).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = Resource.class, names = "MEDIA", mode = EnumSource.Mode.EXCLUDE)
    void allowsMovingBetweenStoresOfTheSameProfile(Resource resource) {
        var fixture = fixture(resource);

        assertDoesNotThrow(() -> fixture.update(fixture.context.secondOwnStore));

        fixture.verifyUpdate();
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void allowsDeletingOwnResource(Resource resource) {
        var fixture = fixture(resource);

        assertDoesNotThrow(fixture::delete);

        fixture.verifyDelete();
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void publicEntityReadsDoNotRequireAProfile(Resource resource) {
        var fixture = fixture(resource);
        clearInvocations(fixture.context.currentUser, fixture.context.profiles);

        assertDoesNotThrow(() -> fixture.service.findEntityByIdOrElseThrowException(fixture.entity.getId()));

        verifyNoInteractions(fixture.context.currentUser, fixture.context.profiles);
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void missingProfileCannotCreateResources(Resource resource) {
        var fixture = fixture(resource);
        when(fixture.context.profiles.requireCompleteProfileByCognitoSub("owner"))
                .thenThrow(new ProfileNotFoundException("Missing profile"));

        assertThrows(ProfileNotFoundException.class, () -> fixture.create(fixture.context.ownStore));

        fixture.assertNoMutation();
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void incompleteProfileCannotUpdateResources(Resource resource) {
        var fixture = fixture(resource);
        when(fixture.context.profiles.requireCompleteProfileByCognitoSub("owner"))
                .thenThrow(new ProfileIncompleteException("Incomplete profile"));

        assertThrows(ProfileIncompleteException.class, () -> fixture.update(fixture.context.ownStore));

        fixture.assertNoMutation();
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void anonymousCallerCannotDeleteResources(Resource resource) {
        var fixture = fixture(resource);
        when(fixture.context.currentUser.currentCognitoSub())
                .thenThrow(new InsufficientAuthenticationException("Sign in required"));

        assertThrows(InsufficientAuthenticationException.class, fixture::delete);

        fixture.assertNoMutation();
    }

    @Test
    void productCannotLinkToAnotherStoresCategoryEvenWhenBothStoresAreOwned() {
        var fixture = fixture(Resource.PRODUCT);
        fixture.context.categories.get(fixture.context.ownStore.getId())
                .setStore(fixture.context.secondOwnStore);

        assertThrows(AccessDeniedException.class, () -> fixture.create(fixture.context.ownStore));
        assertThrows(AccessDeniedException.class, () -> fixture.update(fixture.context.ownStore));

        fixture.assertNoMutation();
    }

    @Test
    void productCannotLinkToForeignCategory() {
        var fixture = fixture(Resource.PRODUCT);
        fixture.context.categories.get(fixture.context.ownStore.getId())
                .setStore(fixture.context.foreignStore);

        assertThrows(AccessDeniedException.class, () -> fixture.update(fixture.context.ownStore));

        fixture.assertNoMutation();
    }

    @Test
    void publicProductDependencyHookCannotReassignForeignProduct() {
        var fixture = fixture(Resource.PRODUCT);
        fixture.assignStore.accept(fixture.context.foreignStore);
        var request = ProductRequestDto.builder().storeId(fixture.context.ownStore.getId())
                .categoryId(fixture.context.categories.get(fixture.context.ownStore.getId()).getId())
                .attributeSchemaCode("schema").build();

        assertThrows(AccessDeniedException.class, () ->
                ((ProductServiceImpl) fixture.service).setEntityDependencies((Product) fixture.entity, request));

        assertEquals(fixture.context.foreignStore, ((Product) fixture.entity).getStore());
        fixture.assertNoMutation();
    }

    @Test
    void mediaUpdatesRemainUnsupportedForAuthorizedOwners() {
        var fixture = fixture(Resource.MEDIA);

        assertThrows(UnsupportedOperationException.class, () -> fixture.update(fixture.context.ownStore));

        fixture.assertNoMutation();
    }

    @Test
    void asyncStockUpdatesDoNotRequireInteractiveIdentity() {
        var fixture = fixture(Resource.VARIANT);
        var variant = (ProductVariant) fixture.entity;
        variant.setStockQuantity(8);
        when(fixture.context.currentUser.currentCognitoSub())
                .thenThrow(new InsufficientAuthenticationException("No async identity"));
        clearInvocations(fixture.context.currentUser, fixture.context.profiles);

        ((ProductVariantServiceImpl) fixture.service).updateVariantStock(variant.getId(), 3);

        assertEquals(5, variant.getStockQuantity());
        verifyNoInteractions(fixture.context.currentUser, fixture.context.profiles);
        verify((ProductVariantRepository) fixture.repository).save(variant);
    }

    private Fixture<?, ?, ?> fixture(Resource resource) {
        var context = new Context();
        return switch (resource) {
            case PRODUCT -> {
                var repository = mock(ProductRepository.class);
                var mapper = spy(new ProductMapper());
                var entity = context.product(context.ownStore);
                var service = new ProductServiceImpl(repository, mapper, context.stores,
                        context.categoryService, context.schemas, context.access);
                yield new Fixture<>(context, entity, repository, mapper, service, store ->
                        ProductRequestDto.builder().name("Updated").description("Description")
                                .price(BigDecimal.TEN).active(true).storeId(store.getId())
                                .categoryId(context.categories.get(store.getId()).getId())
                                .attributeSchemaCode("schema").build(), entity::setStore);
            }
            case VARIANT -> {
                var repository = mock(ProductVariantRepository.class);
                var mapper = spy(new ProductVariantMapper());
                var entity = new ProductVariant();
                entity.setProduct(context.product(context.ownStore));
                entity.setStockQuantity(8);
                var service = new ProductVariantServiceImpl(repository, mapper, context.products,
                        context.validator, context.access);
                yield new Fixture<>(context, entity, repository, mapper, service, store ->
                        ProductVariantRequestDto.builder().productId(context.parents.get(store.getId()).getId())
                                .stockQuantity(4).attributes(Map.of()).build(),
                        store -> entity.getProduct().setStore(store));
            }
            case MEDIA -> {
                var repository = mock(ProductMediaResourceRepository.class);
                var mapper = spy(new ProductMediaResourceMapper());
                var entity = new ProductMediaResource();
                entity.setProduct(context.product(context.ownStore));
                var service = new ProductMediaResourceServiceImpl(repository, mapper, context.products,
                        context.media, context.access);
                yield new Fixture<>(context, entity, repository, mapper, service, store ->
                        ProductMediaResourceRequestDto.builder()
                                .productId(context.parents.get(store.getId()).getId()).primaryImage(true)
                                .file(new MockMultipartFile("file", "image.png", "image/png", new byte[]{1}))
                                .build(), store -> entity.getProduct().setStore(store));
            }
            case CATEGORY -> {
                var repository = mock(CategoryRepository.class);
                var mapper = spy(new CategoryMapper());
                var entity = new Category();
                entity.setStore(context.ownStore);
                var service = new CategoryServiceImpl(repository, mapper, context.stores, context.access);
                yield new Fixture<>(context, entity, repository, mapper, service,
                        store -> new CategoryRequestDto("Updated", store.getId()), entity::setStore);
            }
            case LOCATION -> {
                var repository = mock(LocationRepository.class);
                var mapper = spy(new LocationMapper());
                var entity = new Location();
                entity.setStore(context.ownStore);
                var service = new LocationServiceImpl(repository, mapper, context.stores, context.access);
                yield new Fixture<>(context, entity, repository, mapper, service, store ->
                        LocationRequestDto.builder().name("Updated").address("Address")
                                .storeId(store.getId()).primary(true).build(), entity::setStore);
            }
        };
    }

    private static final class Fixture<E extends BaseJpaEntity, Q, R> {
        private final Context context;
        private final E entity;
        private final JpaRepository<E, UUID> repository;
        private final DtoMapper<E, Q, R> mapper;
        private final Service<E, UUID, Q, R> service;
        private final Function<Store, Q> request;
        private final Consumer<Store> assignStore;

        private Fixture(Context context, E entity, JpaRepository<E, UUID> repository,
                        DtoMapper<E, Q, R> mapper, Service<E, UUID, Q, R> service,
                        Function<Store, Q> request, Consumer<Store> assignStore) {
            this.context = context;
            this.entity = entity;
            this.repository = repository;
            this.mapper = mapper;
            this.service = service;
            this.request = request;
            this.assignStore = assignStore;
            entity.setId(UUID.randomUUID());
            when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        private void create(Store store) {
            service.create(request.apply(store));
        }

        private void update(Store store) {
            service.update(entity.getId(), request.apply(store));
        }

        private void delete() {
            service.deleteById(entity.getId());
        }

        private void assertNoMutation() {
            verifyNoInteractions(mapper, context.media, context.schemas, context.validator);
            verify(repository, never()).save(any());
            verify(repository, never()).delete(any());
            verify(repository, never()).deleteAll();
            if (repository instanceof LocationRepository locations) {
                verify(locations, never()).unsetPrimaryByStoreId(any());
            }
            if (repository instanceof ProductMediaResourceRepository mediaResources) {
                verify(mediaResources, never()).unsetPrimaryImageByProductId(any());
                verify(mediaResources, never()).findMaxSortOrderByProductId(any());
            }
        }

        private void verifyUpdate() {
            verify(mapper).updateEntityFromDto(any(), any());
            verify(repository).save(entity);
        }

        private void verifyDelete() {
            verify(repository).delete(entity);
        }
    }

    private static final class Context {
        private final CurrentUserUtil currentUser = mock(CurrentUserUtil.class);
        private final UserProfileService profiles = mock(UserProfileService.class);
        private final StoreService stores = mock(StoreService.class);
        private final ProductService products = mock(ProductService.class);
        private final CategoryService categoryService = mock(CategoryService.class);
        private final AttributeSchemaService schemas = mock(AttributeSchemaService.class);
        private final AttributeSchemaValidator validator = mock(AttributeSchemaValidator.class);
        private final MediaResourceService media = mock(MediaResourceService.class);
        private final StoreAccessUtil access = new StoreAccessUtil(currentUser, profiles);
        private final UserProfile profile = profile();
        private final Store ownStore = store(profile);
        private final Store secondOwnStore = store(profile);
        private final Store foreignStore = store(profile());
        private final Map<UUID, Product> parents = new java.util.HashMap<>();
        private final Map<UUID, Category> categories = new java.util.HashMap<>();
        private final AttributeSchema schema = new AttributeSchema();

        private Context() {
            when(currentUser.currentCognitoSub()).thenReturn("owner");
            when(profiles.requireCompleteProfileByCognitoSub("owner")).thenReturn(profile);
            schema.setSchemaJson("{}");
            when(schemas.findEntityByCode("schema")).thenReturn(schema);
            var mediaResponse = mock(MediaResourceResponseDto.class);
            when(mediaResponse.getId()).thenReturn(UUID.randomUUID());
            when(media.create(any())).thenReturn(mediaResponse);
            for (var store : List.of(ownStore, secondOwnStore, foreignStore)) {
                when(stores.findEntityByIdOrElseThrowException(store.getId())).thenReturn(store);
                var category = new Category();
                category.setId(UUID.randomUUID());
                category.setStore(store);
                categories.put(store.getId(), category);
                when(categoryService.findEntityByIdOrElseThrowException(category.getId())).thenReturn(category);
                var product = product(store);
                parents.put(store.getId(), product);
                when(products.findEntityByIdOrElseThrowException(product.getId())).thenReturn(product);
            }
        }

        private Product product(Store store) {
            var product = new Product();
            product.setId(UUID.randomUUID());
            product.setStore(store);
            product.setCategory(categories.get(store.getId()));
            product.setAttributeSchema(schema);
            return product;
        }

        private static UserProfile profile() {
            var profile = new UserProfile();
            profile.setId(UUID.randomUUID());
            return profile;
        }

        private static Store store(UserProfile profile) {
            var store = new Store();
            store.setId(UUID.randomUUID());
            store.setUserProfile(profile);
            return store;
        }
    }
}
