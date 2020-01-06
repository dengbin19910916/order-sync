package com.purcotton.omni.ordersync.core;

import com.purcotton.omni.ordersync.core.event.AdditionEvent;
import com.purcotton.omni.ordersync.domain.Property;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;

@Slf4j
@Component
@Order(1)
public class ScheduleManager implements ApplicationListener<ApplicationEvent> {

    private final GenericApplicationContext applicationContext;
    private final RestTemplate restTemplate;
    private final Scheduler scheduler;

    public ScheduleManager(ApplicationContext applicationContext,
                           RestTemplate restTemplate,
                           Scheduler scheduler) {
        this.applicationContext = (GenericApplicationContext) applicationContext;
        this.restTemplate = restTemplate;
        this.scheduler = scheduler;
    }

    @SneakyThrows
    private void addJob(Property property) {
        JobDetail jobDetail = JobBuilder.newJob(Synchronizer.class).build();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        jobDataMap.put("applicationContext", applicationContext);
        jobDataMap.put("restTemplate", restTemplate);
        jobDataMap.put("property", property);

        Trigger trigger = TriggerBuilder.newTrigger()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(property.getTriggerInterval())
                                .repeatForever()

                )
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
    }

    @SneakyThrows
    @Override
    public void onApplicationEvent(@Nonnull ApplicationEvent event) {
        if (event instanceof AdditionEvent) {
            addJob((Property) event.getSource());
        }
    }
}
