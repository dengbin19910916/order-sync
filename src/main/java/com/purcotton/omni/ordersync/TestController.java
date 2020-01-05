package com.purcotton.omni.ordersync;

import com.google.common.base.CaseFormat;
import com.purcotton.omni.ordersync.core.Synchronizer;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final ApplicationContext context;

    public TestController(ApplicationContext context) {
        this.context = context;
    }

    @GetMapping("/tt")
    public void test() throws JobExecutionException {
        Synchronizer synchronizer = context.getBean("synchronizer", Synchronizer.class);
        synchronizer.execute(null);
    }

    public static void main(String[] args) {
        System.out.println(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, "test_data"));
    }
}
