package com.purcotton.omni.ordersync.config;

import com.purcotton.omni.ordersync.core.ScheduleScanner;
import com.purcotton.omni.ordersync.core.SyncScheduler;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
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
                                .withIntervalInSeconds(10)
                                .repeatForever()
                )
                .build();
    }

    @Bean
    public JobDetail scheduleManagerJobDetail() {
        return JobBuilder.newJob(ScheduleScanner.class)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger scheduleManagerTrigger(@Qualifier("scheduleManagerJobDetail") JobDetail jobDetail) {
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
    public CuratorFramework client() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .retryPolicy(retryPolicy)
                .sessionTimeoutMs(60000)
                .connectionTimeoutMs(3000)
                .build();
        client.start();
        return client;
    }
}
