package org.bazar.vektrlabs.repository;

import org.bazar.vektrlabs.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<List<Category>> findByStoreId(UUID storeId);

    Optional<Category> findByStoreIdAndName(
            UUID storeId,
            String name
    );

    boolean existsByStoreIdAndName(
            UUID storeId,
            String name
    );
}