package com.purcotton.omni.ordersync.core.event;

import com.purcotton.omni.ordersync.domain.Property;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class RegisterEvent extends ApplicationEvent {

    @Getter
    private boolean leader;

    public RegisterEvent(Property property) {
        super(property);
    }

    public RegisterEvent(Property property, boolean leader) {
        super(property);
        this.leader = leader;
    }
}
