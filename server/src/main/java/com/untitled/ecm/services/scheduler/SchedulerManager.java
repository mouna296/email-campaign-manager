package com.untitled.ecm.services.scheduler;

import io.dropwizard.lifecycle.Managed;
import lombok.Getter;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;

public class SchedulerManager implements Managed {
    @Getter
    private static Scheduler quartzScheduler;
    private final SchedulerFactory schedulerFactory;

    public SchedulerManager(SchedulerFactory sf) throws SchedulerException {
        if (sf.getAllSchedulers().size() > 0) {
            throw new RuntimeException("One or more scheduler is/are already running. only one scheduler must be running at a time");
        }

        schedulerFactory = sf;
        quartzScheduler = sf.getScheduler();
    }


    @Override
    public void start() throws Exception {
        quartzScheduler.start();
    }

    @Override
    public void stop() throws Exception {
        for (Scheduler scheduler : this.schedulerFactory.getAllSchedulers()) {
            scheduler.shutdown(true);
        }
    }
}
