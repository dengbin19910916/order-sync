package com.purcotton.omni.ordersync.core.event;

import org.springframework.context.ApplicationEvent;

public class RoleChangedEvent extends ApplicationEvent {

    private boolean leader;

    public RoleChangedEvent(Object source) {
        super(source);
    }
}
