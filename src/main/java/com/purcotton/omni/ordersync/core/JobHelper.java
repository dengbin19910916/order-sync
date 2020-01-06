package com.purcotton.omni.ordersync.core;

import com.alibaba.fastjson.JSON;
import com.purcotton.omni.ordersync.core.event.AdditionEvent;
import com.purcotton.omni.ordersync.core.event.RegisterEvent;
import com.purcotton.omni.ordersync.domain.Property;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.quartz.*;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;

import static org.apache.zookeeper.CreateMode.PERSISTENT;

@Slf4j
public class JobHelper implements ApplicationListener<ApplicationEvent> {

    public static final String JOB_PATH = "/omni/sync/job";
    public static final String LEADER_PATH = "/omni/sync/leader";

    private final GenericApplicationContext applicationContext;
    private final RestTemplate restTemplate;
    private final CuratorFramework client;
    private final Scheduler scheduler;

    public JobHelper(GenericApplicationContext applicationContext,
                     RestTemplate restTemplate,
                     CuratorFramework client,
                     Scheduler scheduler) {
        this.applicationContext = applicationContext;
        this.restTemplate = restTemplate;
        this.client = client;
        this.scheduler = scheduler;
    }

    @SneakyThrows
    @Override
    public void onApplicationEvent(@Nonnull ApplicationEvent event) {
        if (event instanceof RegisterEvent) {
            if (SyncContext.isLeader()) {
                Property property = (Property) event.getSource();
                String beanName = property.getBeanName();

                String path = JOB_PATH + "/" + beanName;
                Stat stat = client.checkExists().forPath(path);
                if (stat == null) {
                    client.create().creatingParentContainersIfNeeded()
                            .withMode(PERSISTENT).forPath(path);
                }
                log.debug("Register: " + new String(JSON.toJSONBytes(property)));
                client.setData().forPath(path, JSON.toJSONBytes(property));
            }
        }

        if (event instanceof AdditionEvent) {
            if (SyncContext.isLeader()) {
                addJob((Property) event.getSource());
            }
        }
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
}
