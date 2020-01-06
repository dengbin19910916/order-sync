package com.purcotton.omni.ordersync.web;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(("/jobs"))
public class JobController {

    private final Scheduler scheduler;

    public JobController(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PutMapping("/{name}/interrupt")
    public void interrupt(@PathVariable String name) throws SchedulerException {
        JobKey jobKey = new JobKey(name);
        scheduler.interrupt(jobKey);
    }

    @DeleteMapping("/{name}/stop")
    public void stop(@PathVariable String name) throws SchedulerException {
        JobKey jobKey = new JobKey(name);
        scheduler.pauseJob(jobKey);
        scheduler.interrupt(jobKey);
    }

    @PutMapping("/{name}/resume")
    public void resume(@PathVariable String name) throws SchedulerException {
        JobKey jobKey = new JobKey(name);
        scheduler.resumeJob(jobKey);
    }
}
