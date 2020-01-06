package com.purcotton.omni.ordersync.core;

import com.alibaba.fastjson.JSON;
import com.purcotton.omni.ordersync.core.event.AdditionEvent;
import com.purcotton.omni.ordersync.domain.Property;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.purcotton.omni.ordersync.core.JobHelper.JOB_PATH;

@Slf4j
@Component
@Order(1)
public class JobManager implements ApplicationRunner {

    private final GenericApplicationContext applicationContext;
    private final CuratorFramework client;

    public JobManager(ApplicationContext applicationContext,
                      CuratorFramework client) {
        this.applicationContext = (GenericApplicationContext) applicationContext;
        this.client = client;
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

}
