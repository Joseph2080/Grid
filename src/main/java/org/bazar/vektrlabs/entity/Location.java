package org.bazar.vektrlabs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jericho.common.entity.BaseJpaEntity;

@Entity
@Table(name = "locations")
@Getter
@Setter
public class Location extends BaseJpaEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column
    private String city;

    @Column
    private String country;

    @Column(length = 50)
    private String postalCode;

    @Column(length = 100)
    private String phone;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(name = "is_primary")
    private boolean primary = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
}