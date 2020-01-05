package com.purcotton.omni.ordersync.core;

import com.alibaba.fastjson.JSON;
import com.purcotton.omni.ordersync.core.event.AdditionEvent;
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

import java.util.List;
import java.util.UUID;

import static com.purcotton.omni.ordersync.core.JobHelper.JOB_PATH;
import static com.purcotton.omni.ordersync.core.JobHelper.LEADER_PATH;
import static org.apache.zookeeper.CreateMode.PERSISTENT;

@Slf4j
@Component
@Order(2)
public class Leader implements ApplicationRunner {


    private final GenericApplicationContext applicationContext;
    private final CuratorFramework client;

    private volatile boolean leader;

    public Leader(GenericApplicationContext applicationContext,
                  CuratorFramework client) {
        this.applicationContext = applicationContext;
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var leaderLatch = new LeaderLatch(client, LEADER_PATH,
                UUID.randomUUID().toString().replaceAll("-", ""));
        leaderLatch.addListener(new LeaderLatchListener() {
            @SneakyThrows
            @Override
            public void isLeader() {
                SyncContext.setLeader(true);
                if (log.isErrorEnabled()) {
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
                    applicationContext.publishEvent(new AdditionEvent(property, true));
                }
            }

            @Override
            public void notLeader() {
                SyncContext.setLeader(false);
                if (log.isDebugEnabled()) {
                    log.debug("Currently run as slave");
                }
            }
        });
        leaderLatch.start();
    }

    public synchronized boolean isLeader() {
        return leader;
    }

    public synchronized void setLeader(boolean leader) {
        this.leader = leader;
    }

}
