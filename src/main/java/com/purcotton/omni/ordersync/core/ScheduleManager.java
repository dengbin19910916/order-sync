package com.purcotton.omni.ordersync.core;

import com.alibaba.fastjson.JSON;
import com.purcotton.omni.ordersync.core.event.AdditionEvent;
import com.purcotton.omni.ordersync.domain.Property;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
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
import java.util.List;
import java.util.UUID;

import static com.purcotton.omni.ordersync.core.JobHelper.JOB_PATH;
import static com.purcotton.omni.ordersync.core.JobHelper.LEADER_PATH;
import static org.apache.zookeeper.CreateMode.PERSISTENT;

@Slf4j
@Component
@Order(1)
public class ScheduleManager implements ApplicationRunner, ApplicationListener<ApplicationEvent> {

    private final GenericApplicationContext applicationContext;
    private final RestTemplate restTemplate;
    private final CuratorFramework client;
    private final Scheduler scheduler;

    private volatile boolean leader;

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
                    try {
                        Property property = JSON.parseObject(event.getData().getData(), Property.class);
                        applicationContext.publishEvent(new AdditionEvent(property));
                    } catch (Exception ignored) {
                    }
                    break;
                case CHILD_REMOVED:
                    throw new RuntimeException("未实现任务删除");
                default:
                    break;
            }
        });
        cache.start();

        var leaderLatch = new LeaderLatch(client, LEADER_PATH,
                UUID.randomUUID().toString().replaceAll("-", ""));
        leaderLatch.addListener(new LeaderLatchListener() {
            @SneakyThrows
            @Override
            public void isLeader() {
                setLeader(true);
                if (log.isDebugEnabled()) {
                    log.debug("Currently run as leader");
                }

                Stat stat = client.checkExists().forPath(JOB_PATH);
                if (stat == null) {
                    client.create().creatingParentContainersIfNeeded()
                            .withMode(PERSISTENT).forPath(JOB_PATH);
                }

                List<String> childPaths = client.getChildren().forPath(JOB_PATH);
                for (String childPath : childPaths) {
                    Property property = JSON.parseObject(
                            client.getData().forPath("/omni/sync/job/" + childPath), Property.class);
                    addJob(property);
                }
            }

            @Override
            public void notLeader() {
                if (log.isDebugEnabled()) {
                    log.debug("Currently run as slave");
                }
                setLeader(false);
            }
        });
        leaderLatch.start();
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
//        if (event instanceof RegisterEvent) {
//            Property property = (Property) event.getSource();
//            String beanName = property.getBeanName();
//            String path = JOB_PATH + "/" + beanName;
//
//            Stat stat = client.checkExists().forPath(path);
//            if (stat == null) {
//                client.create().creatingParentContainersIfNeeded()
//                        .withMode(PERSISTENT).forPath(path);
//            }
//            client.setData().forPath(path, JSON.toJSONBytes(property));
//        }

        if (event instanceof AdditionEvent) {
            addJob((Property) event.getSource());
        }
    }

    public synchronized boolean isLeader() {
        return leader;
    }

    public synchronized void setLeader(boolean leader) {
        this.leader = leader;
    }
}
