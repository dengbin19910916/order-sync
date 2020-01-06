package com.purcotton.omni.ordersync.core;

import com.purcotton.omni.ordersync.domain.Property;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncContext {

    @Getter
    private static volatile List<Property> properties = Collections.synchronizedList(new ArrayList<>());

    private static volatile boolean leader;

    public synchronized static boolean isLeader() {
        return leader;
    }

    public synchronized static void setLeader(boolean leader) {
        SyncContext.leader = leader;
    }

    public synchronized static void addAllProperty(List<Property> properties) {
        properties.addAll(properties);
    }
}
