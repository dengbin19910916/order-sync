package com.purcotton.omni.ordersync.core.event;

import com.purcotton.omni.ordersync.domain.Property;
import org.springframework.context.ApplicationEvent;

public class JobAddedEvent extends ApplicationEvent {

    public JobAddedEvent(Property property) {
        super(property);
    }
}
