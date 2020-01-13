package com.purcotton.omni.ordersync.data;

import com.purcotton.omni.ordersync.domain.OmniOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OmniOrderRepository extends JpaRepository<OmniOrder, Long> {

    Optional<OmniOrder> findByCid(String cid);
}
