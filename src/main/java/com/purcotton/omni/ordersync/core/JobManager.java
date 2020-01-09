package com.purcotton.omni.ordersync.core;

import com.purcotton.omni.ordersync.core.event.JobAdditionEvent;
import com.purcotton.omni.ordersync.domain.Property;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;

@Slf4j
@Component
public class JobManager implements ApplicationListener<ApplicationEvent> {

    private final GenericApplicationContext applicationContext;
    private final RestTemplate restTemplate;
    private final Scheduler scheduler;

    public JobManager(ApplicationContext applicationContext,
                      RestTemplate restTemplate,
                      Scheduler scheduler) {
        this.applicationContext = (GenericApplicationContext) applicationContext;
        this.restTemplate = restTemplate;
        this.scheduler = scheduler;
    }

    @SneakyThrows
    private synchronized void addJob(Property property) {
        JobKey jobKey = new JobKey(property.getBeanName());
        JobDetail jobDetail = scheduler.checkExists(jobKey)
                ? scheduler.getJobDetail(jobKey)
                : JobBuilder.newJob(Synchronizer.class)
                .withIdentity(jobKey)
                .build();

        TriggerKey triggerKey = new TriggerKey(property.getBeanName());
        Trigger trigger = scheduler.checkExists(triggerKey)
                ? scheduler.getTrigger(triggerKey)
                : TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(property.getTriggerInterval())
                                .repeatForever()

                )
                .build();

        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        jobDataMap.put("applicationContext", applicationContext);
        jobDataMap.put("restTemplate", restTemplate);
        jobDataMap.put("property", property);

        if (!scheduler.checkExists(jobKey) && !scheduler.checkExists(triggerKey)) {
            scheduler.scheduleJob(jobDetail, trigger);
        }
        scheduler.start();
    }

    @SneakyThrows
    @Override
    public void onApplicationEvent(@Nonnull ApplicationEvent event) {
        if (event instanceof JobAdditionEvent) {
            addJob((Property) event.getSource());
        }
    }
}
