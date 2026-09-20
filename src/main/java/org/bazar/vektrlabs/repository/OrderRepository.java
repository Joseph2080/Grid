package org.bazar.vektrlabs.repository;

import org.bazar.vektrlabs.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {"items", "items.variant"})
    Optional<Order> findById(UUID id);

    @EntityGraph(attributePaths = {"items", "items.variant"})
    Page<Order> findAll(Pageable pageable);

}