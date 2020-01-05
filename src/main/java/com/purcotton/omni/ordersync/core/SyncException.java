package com.purcotton.omni.ordersync.core;

import org.springframework.core.NestedRuntimeException;

public class SyncException extends NestedRuntimeException {

    public SyncException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
