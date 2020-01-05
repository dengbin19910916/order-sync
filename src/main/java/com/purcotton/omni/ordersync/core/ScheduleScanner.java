package com.purcotton.omni.ordersync.core;

import com.purcotton.omni.ordersync.core.event.AdditionEvent;
import com.purcotton.omni.ordersync.core.event.RegisterEvent;
import com.purcotton.omni.ordersync.data.PropertyRepository;
import com.purcotton.omni.ordersync.domain.Property;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;

import static com.purcotton.omni.ordersync.core.JobHelper.JOB_PATH;

@Slf4j
@Component
public class ScheduleScanner extends QuartzJobBean {

    private GenericApplicationContext applicationContext;
    private PropertyRepository propertyRepository;
    private CuratorFramework client;

    @SneakyThrows
    @Override
    protected void executeInternal(@Nonnull JobExecutionContext context) {
        List<Property> properties = propertyRepository.findAll();
        for (Property property : properties) {
            String beanName = property.getBeanName();

            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                    .genericBeanDefinition(Synchronizer.class);
            BeanDefinition beanDefinition = builder.getBeanDefinition();

            if (!applicationContext.isBeanNameInUse(beanName)) {
                applicationContext.registerBeanDefinition(beanName, beanDefinition);
                Stat stat = client.checkExists().forPath(JOB_PATH + "/" + beanName);
                if (stat == null) {
                    applicationContext.publishEvent(new RegisterEvent(property));
                    applicationContext.publishEvent(new AdditionEvent(property));
                }
            }
        }
    }

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = (GenericApplicationContext) applicationContext;
    }

    @Autowired
    public void setPropertyRepository(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Autowired
    public void setClient(CuratorFramework client) {
        this.client = client;
    }
}