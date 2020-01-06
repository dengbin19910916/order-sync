package com.purcotton.omni.ordersync.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Schedule schedule;

    @Lob
    private String message;

    @Column(columnDefinition = "integer")
    private Integer totalNumber;

    @Column(columnDefinition = "integer")
    private Integer succeedNumber;

    @Column(columnDefinition = "integer")
    private Integer failedNumber;

    @Column(precision = 19, scale = 2)
    private Double pullTime;

    @Column(precision = 19, scale = 2)
    private Double saveTime;

    private LocalDateTime createdTime;

    public Log(Schedule schedule, Integer totalNumber,
               Integer succeedNumber, Integer failedNumber,
               Double pullTime, Double saveTime) {
        this.schedule = schedule;
        this.totalNumber = totalNumber;
        this.succeedNumber = succeedNumber;
        this.failedNumber = failedNumber;
        this.pullTime = pullTime;
        this.saveTime = saveTime;
        this.createdTime = LocalDateTime.now();
    }
}
