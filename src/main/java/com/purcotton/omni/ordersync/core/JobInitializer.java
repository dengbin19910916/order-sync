package com.purcotton.omni.ordersync.core;

import com.purcotton.omni.ordersync.core.event.AdditionEvent;
import com.purcotton.omni.ordersync.core.event.RegisterEvent;
import com.purcotton.omni.ordersync.data.PropertyRepository;
import com.purcotton.omni.ordersync.domain.Property;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.purcotton.omni.ordersync.core.JobHelper.JOB_PATH;
import static org.apache.zookeeper.CreateMode.PERSISTENT;

@Slf4j
@Component
@Order(1)
public class JobInitializer implements ApplicationRunner {

    private final GenericApplicationContext applicationContext;
    private final PropertyRepository propertyRepository;
    private final CuratorFramework client;

    public JobInitializer(GenericApplicationContext applicationContext,
                          PropertyRepository propertyRepository,
                          CuratorFramework client) {
        this.applicationContext = applicationContext;
        this.propertyRepository = propertyRepository;
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (SyncContext.isLeader()) {
            createPath();

            var properties = propertyRepository.findAll();
            SyncContext.addAllProperty(properties);

            for (Property property : properties) {
                Stat stat = client.checkExists().forPath(JOB_PATH + "/" + property.getBeanName());
                if (stat == null) {
                    applicationContext.publishEvent(new RegisterEvent(property));
                }
                applicationContext.publishEvent(new AdditionEvent(property, true));
            }
        }
    }

    @SneakyThrows
    private void createPath() {
        if (client.checkExists().forPath(JobHelper.JOB_PATH) == null) {
            client.create().creatingParentContainersIfNeeded()
                    .withMode(PERSISTENT).forPath(JobHelper.JOB_PATH);
        }
    }
}
