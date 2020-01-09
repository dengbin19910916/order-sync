package com.purcotton.omni.ordersync.core.event;

import com.purcotton.omni.ordersync.domain.Property;
import org.springframework.context.ApplicationEvent;

public class JobAdditionEvent extends ApplicationEvent {

    public JobAdditionEvent(Property property) {
        super(property);
    }
}
