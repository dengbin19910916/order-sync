package com.purcotton.omni.ordersync.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
public class Error {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Schedule schedule;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private String data;

    @Lob
    private String message;

    private LocalDateTime createdTime;

    public Error(Schedule schedule, String data, String message) {
        this.schedule = schedule;
        this.data = data;
        this.message = message;
        this.createdTime = LocalDateTime.now();
    }
}
