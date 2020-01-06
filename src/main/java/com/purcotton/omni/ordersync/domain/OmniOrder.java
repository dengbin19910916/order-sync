package com.purcotton.omni.ordersync.domain;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(indexes = {
        @Index(columnList = "cid"),
        @Index(columnList = "tid"),
        @Index(columnList = "rid"),
        @Index(name="idx_order_property_cid",columnList = "property_id"),
        @Index(name="idx_order_property_cid",columnList = "cid")
})
public class OmniOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String cid;

    @Column(length = 64)
    private String tid;

    @Column(length = 64)
    private String rid;

    @Column(columnDefinition = "json")
    private String data;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdTime;

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedTime;

    @Column(columnDefinition = "datetime")
    private LocalDateTime syncCreatedTime;

    @Column(columnDefinition = "datetime")
    private LocalDateTime syncUpdatedTime;

    @ManyToOne
    private Property property;
}
