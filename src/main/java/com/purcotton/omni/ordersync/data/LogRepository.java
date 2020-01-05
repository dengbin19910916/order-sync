package com.purcotton.omni.ordersync.data;

import com.purcotton.omni.ordersync.domain.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Long> {
}
