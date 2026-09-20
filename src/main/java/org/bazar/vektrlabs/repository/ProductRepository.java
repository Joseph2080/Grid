package org.bazar.vektrlabs.repository;

import org.bazar.vektrlabs.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<List<Product>> findByActiveTrue();

    @Query("SELECT p FROM Product p JOIN p.attributes a WHERE KEY(a) = :key")
    Optional<List<Product>> findByAttributeKey(@Param("key") String key);

    @Query("SELECT p FROM Product p JOIN p.attributes a WHERE KEY(a) = :key AND VALUE(a) = :value")
    Optional<List<Product>> findByAttribute(@Param("key") String key, @Param("value") String value);

    Optional<List<Product>> findByStoreId(UUID storeId);

    Optional<List<Product>> findByCategoryId(UUID categoryId);
}