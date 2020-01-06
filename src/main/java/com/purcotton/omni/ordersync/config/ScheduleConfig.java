package com.purcotton.omni.ordersync.config;

import com.purcotton.omni.ordersync.core.SynchronizerScanner;
import com.purcotton.omni.ordersync.core.SyncScheduler;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScheduleConfig {

    @Bean
    public JobDetail syncSchedulerJobDetail() {
        return JobBuilder.newJob(SyncScheduler.class)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger syncSchedulerTrigger(@Qualifier("syncSchedulerJobDetail") JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(1)
                                .repeatForever()
                )
                .build();
    }

    @Bean
    public JobDetail synchronizerScannerJobDetail() {
        return JobBuilder.newJob(SynchronizerScanner.class)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger synchronizerManagerTrigger(
            @Qualifier("synchronizerScannerJobDetail") JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(1)
                                .repeatForever()
                )
                .build();
    }
}
