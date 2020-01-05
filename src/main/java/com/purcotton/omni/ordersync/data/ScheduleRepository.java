package com.purcotton.omni.ordersync.data;

import com.purcotton.omni.ordersync.domain.Property;
import com.purcotton.omni.ordersync.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long>,
        JpaSpecificationExecutor<Schedule> {

    Optional<Schedule> findFirstByPropertyAndCompletedOrderByStartTime(
            Property property, boolean completed);

    Optional<Schedule> findFirstByPropertyOrderByStartTimeDesc(Property property);
}
