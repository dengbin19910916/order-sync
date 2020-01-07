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
        @Index(name = "idx_order_property_cid", columnList = "property_id"),
        @Index(name = "idx_order_property_cid", columnList = "cid")
})
//@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)  // PostgresQL
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

//    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private String data;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private LocalDateTime syncCreatedTime;

    private LocalDateTime syncUpdatedTime;

    @ManyToOne
    private Property property;
}
