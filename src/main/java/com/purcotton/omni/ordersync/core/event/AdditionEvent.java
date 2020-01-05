package com.purcotton.omni.ordersync.core.event;

import com.purcotton.omni.ordersync.domain.Property;
import org.springframework.context.ApplicationEvent;

public class AdditionEvent extends ApplicationEvent {

    public AdditionEvent(Property property) {
        super(property);
    }
}
