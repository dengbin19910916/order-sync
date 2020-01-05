package com.purcotton.omni.ordersync.core.event;

import com.purcotton.omni.ordersync.domain.Property;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class AdditionEvent extends ApplicationEvent {

    @Getter
    private boolean leader;

    public AdditionEvent(Property property) {
        super(property);
    }

    public AdditionEvent(Property property, boolean leader) {
        super(property);
        this.leader = leader;
    }
}
