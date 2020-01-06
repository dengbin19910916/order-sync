package com.purcotton.omni.ordersync.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.purcotton.omni.ordersync.core.JobHelper.LEADER_PATH;

@Slf4j
@Component
@Order(0)
public class RoleManager implements ApplicationRunner {

    private final CuratorFramework client;

    public RoleManager(CuratorFramework client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var leaderLatch = new LeaderLatch(client, LEADER_PATH,
                UUID.randomUUID().toString().replaceAll("-", ""));
        leaderLatch.addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() {
                SyncContext.setLeader(true);
            }

            @Override
            public void notLeader() {
                SyncContext.setLeader(false);
            }
        });
        leaderLatch.start();


//        var leaderLatch = new LeaderLatch(client, LEADER_PATH,
//                UUID.randomUUID().toString().replaceAll("-", ""));
//        leaderLatch.addListener(new LeaderLatchListener() {
//            @SneakyThrows
//            @Override
//            public void isLeader() {
//                SyncContext.setLeader(true);
//                if (log.isErrorEnabled()) {
//                    log.debug("Currently run as leader");
//                }
//
//                Stat stat = client.checkExists().forPath(JOB_PATH);
//                if (stat == null) {
//                    client.create().creatingParentContainersIfNeeded()
//                            .withMode(PERSISTENT).forPath(JOB_PATH);
//                }
//
//                List<String> childPaths = client.getChildren().forPath(JOB_PATH);
//                for (String childPath : childPaths) {
//                    Property property = JSON.parseObject(
//                            client.getData().forPath("/omni/sync/job/" + childPath), Property.class);
//                    applicationContext.publishEvent(new AdditionEvent(property, true));
//                }
//            }
//
//            @Override
//            public void notLeader() {
//                SyncContext.setLeader(false);
//                if (log.isDebugEnabled()) {
//                    log.debug("Currently run as slave");
//                }
//            }
//        });
//        leaderLatch.start();
    }

}
