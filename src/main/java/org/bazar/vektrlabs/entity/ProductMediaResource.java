package org.bazar.vektrlabs.entity;

import jakarta.persistence.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jericho.common.entity.BaseJpaEntity;
import org.jericho.mediaresource.entity.MediaResource;

@Entity
@Table(name = "product_media_resources")
@Getter
@Setter
public class ProductMediaResource extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_resource_id", nullable = false)
    private MediaResource mediaResource;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_primary")
    private boolean primaryImage = false;
}