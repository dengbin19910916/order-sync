package com.purcotton.omni.ordersync.data;

import com.purcotton.omni.ordersync.domain.Error;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorRepository extends JpaRepository<Error, Long> {
}
