package com.purcotton.omni.ordersync.core;

import com.purcotton.omni.ordersync.core.event.AdditionEvent;
import com.purcotton.omni.ordersync.data.PropertyRepository;
import com.purcotton.omni.ordersync.domain.Property;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.zookeeper.data.Stat;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.purcotton.omni.ordersync.core.JobHelper.JOB_PATH;
import static com.purcotton.omni.ordersync.core.JobHelper.LEADER_PATH;
import static org.apache.zookeeper.CreateMode.PERSISTENT;

@Slf4j
@Component
@Order(0)
public class SchedulerInitializer implements ApplicationRunner {

    private final GenericApplicationContext applicationContext;
    private final PropertyRepository propertyRepository;
    private final CuratorFramework client;

    public SchedulerInitializer(GenericApplicationContext applicationContext,
                                PropertyRepository propertyRepository,
                                CuratorFramework client) {
        this.applicationContext = applicationContext;
        this.propertyRepository = propertyRepository;
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Stat stat = client.checkExists().forPath(JOB_PATH);
        if (stat == null) {
            client.create().creatingParentContainersIfNeeded()
                    .withMode(PERSISTENT).forPath(JOB_PATH);
        }

        var leaderLatch = new LeaderLatch(client, LEADER_PATH,
                UUID.randomUUID().toString().replaceAll("-", ""));
        leaderLatch.addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() {
                var properties = propertyRepository.findAll();
                for (Property property : properties) {
                    applicationContext.publishEvent(new AdditionEvent(property));
                }
                leaderLatch.removeListener(this);
            }

            @SneakyThrows
            @Override
            public void notLeader() {
            }
        });
        leaderLatch.start();
    }
}
