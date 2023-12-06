package com.untitled.ecm.health;

import com.untitled.ecm.services.scheduler.SchedulerManager;
import com.codahale.metrics.health.HealthCheck;
import org.quartz.Scheduler;

public class SchedulerHealthCheck extends HealthCheck {

    @Override
    protected Result check() throws Exception {

        Scheduler scheduler = SchedulerManager.getQuartzScheduler();
        if (!scheduler.isStarted()) {
            return Result.unhealthy(scheduler.getSchedulerName() + "not running");
        }

        return Result.healthy(scheduler.getSchedulerName() + " is running");
    }
}
