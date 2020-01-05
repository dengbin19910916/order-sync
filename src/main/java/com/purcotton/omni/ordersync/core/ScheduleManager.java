package com.purcotton.omni.ordersync.core;

import com.alibaba.fastjson.JSON;
import com.purcotton.omni.ordersync.core.event.AdditionEvent;
import com.purcotton.omni.ordersync.core.event.RegisterEvent;
import com.purcotton.omni.ordersync.domain.Property;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.data.Stat;
import org.quartz.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;

import static com.purcotton.omni.ordersync.core.JobHelper.JOB_PATH;
import static org.apache.zookeeper.CreateMode.PERSISTENT;

@Slf4j
@Component
@Order(1)
public class ScheduleManager implements ApplicationRunner, ApplicationListener<ApplicationEvent> {

    private final GenericApplicationContext applicationContext;
    private final RestTemplate restTemplate;
    private final CuratorFramework client;
    private final Scheduler scheduler;

    public ScheduleManager(ApplicationContext applicationContext,
                           RestTemplate restTemplate,
                           CuratorFramework client,
                           Scheduler scheduler) {
        this.applicationContext = (GenericApplicationContext) applicationContext;
        this.restTemplate = restTemplate;
        this.client = client;
        this.scheduler = scheduler;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var cache = new PathChildrenCache(
                client, JOB_PATH, true);
        cache.getListenable().addListener((client, event) -> {
            switch (event.getType()) {
                case CHILD_ADDED:
                    log.debug("Path: " + event.getData().getPath());
                    log.debug("Data: " + new String(event.getData().getData()));
                    try {
                        Property property = JSON.parseObject(event.getData().getData(), Property.class);
                        applicationContext.publishEvent(new AdditionEvent(property));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case CHILD_REMOVED:
                    throw new RuntimeException("未实现任务删除");
                default:
                    break;
            }
        });
        cache.start();
    }

    @SneakyThrows
    private void addJob(Property property) {
        JobDetail jobDetail = JobBuilder.newJob(Synchronizer.class).build();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        jobDataMap.put("applicationContext", applicationContext);
        jobDataMap.put("restTemplate", restTemplate);
        jobDataMap.put("property", property);

        // 只有Master执行任务
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

}
