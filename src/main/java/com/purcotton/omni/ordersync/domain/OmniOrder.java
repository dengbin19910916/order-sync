package com.purcotton.omni.ordersync.domain;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class OmniOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cid;

    private String tid;

    private String rid;

    @Column(columnDefinition = "json")
    private String data;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private LocalDateTime syncCreatedTime;

    private LocalDateTime syncUpdatedTime;
}
