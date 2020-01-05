package com.purcotton.omni.ordersync.core;

public class SyncContext {

    private static volatile boolean leader;

    public synchronized static boolean isLeader() {
        return leader;
    }

    public synchronized static void setLeader(boolean leader) {
        SyncContext.leader = leader;
    }
}
