package com.purcotton.omni.ordersync.core;

import com.purcotton.omni.ordersync.data.PropertyRepository;
import com.purcotton.omni.ordersync.data.ScheduleRepository;
import com.purcotton.omni.ordersync.domain.Property;
import com.purcotton.omni.ordersync.domain.Schedule;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

import static java.time.LocalDateTime.now;

@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class SyncScheduler extends QuartzJobBean {

    private PropertyRepository propertyRepository;
    private ScheduleRepository scheduleRepository;

    @Override
    protected void executeInternal(@Nonnull JobExecutionContext context) {
        propertyRepository.findAll().forEach(this::saveNextSchedule);
    }

    private void saveNextSchedule(Property property) {
        if (property.isEnabled()) {
            Optional<Schedule> lastSchedule = scheduleRepository
                    .findFirstByPropertyOrderByStartTimeDesc(property);
            LocalDateTime startTime = lastSchedule
                    .map(Schedule::getEndTime)
                    .orElse(property.getOriginTime());

            Schedule schedule = new Schedule();
            schedule.setProperty(property);
            schedule.setStartTime(startTime.minusSeconds(
                    Math.abs(Objects.requireNonNullElse(property.getStartTimeOffset(), 0))));
            schedule.setStartTimeMillis(getMillis(startTime));
            schedule.setEndTime(startTime.plusSeconds(
                    Objects.requireNonNullElse(property.getTimeInterval(), 60L)));
            schedule.setEndTimeMillis(getMillis(schedule.getEndTime()));
            schedule.setCompleted(false);

            LocalDateTime now = now();
            if (fire(property, schedule, now)) {
                schedule.setCreatedTime(now);
                schedule.setUpdatedTime(now);
                scheduleRepository.save(schedule);
            }
        }
    }

    private boolean fire(Property property, Schedule schedule, LocalDateTime now) {
        long delay = Objects.requireNonNullElse(property.getDelay(), 60);
        return schedule.getEndTime().isBefore(now.minusSeconds(delay));
    }

    private long getMillis(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Autowired
    public void setPropertyRepository(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Autowired
    public void setScheduleRepository(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }
}
