package com.purcotton.omni.ordersync.domain;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Property property;

    @Column(columnDefinition = "datetime")
    private LocalDateTime startTime;

    private Long startTimeMillis;

    @Column(columnDefinition = "datetime")
    private LocalDateTime endTime;

    private Long endTimeMillis;

    private boolean completed;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdTime;

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedTime;

    @OneToMany(mappedBy = "schedule")
    private List<Log> logs;

    @OneToMany(mappedBy = "schedule")
    private List<Error> errors;

    @Override
    public String toString() {
        return "Schedule{" +
                "id=" + id +
                ", startTime=" + startTime +
                ", startTimeMillis=" + startTimeMillis +
                ", endTime=" + endTime +
                ", endTimeMillis=" + endTimeMillis +
                ", completed=" + completed +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                '}';
    }
}
