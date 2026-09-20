package org.bazar.vektrlabs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Index;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jericho.common.entity.BaseJpaEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_store_name",
                        columnNames = {"store_id", "name"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_category_store_id",
                        columnList = "store_id"
                )
        }
)
@Getter
@Setter
public class Category extends BaseJpaEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @OneToMany(mappedBy = "category")
    private List<Product> products = new ArrayList<>();
}