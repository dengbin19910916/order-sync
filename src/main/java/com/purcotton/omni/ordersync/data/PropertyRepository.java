package com.purcotton.omni.ordersync.data;

import com.purcotton.omni.ordersync.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {
}
