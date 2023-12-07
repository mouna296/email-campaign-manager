package com.untitled.ecm.health;

import com.untitled.ecm.core.DakiyaBackGroundTasksManager;
import com.codahale.metrics.health.HealthCheck;

public class BackGroundTaskManagerHealthCheck extends HealthCheck {
    @Override
    protected Result check() throws Exception {
        if (DakiyaBackGroundTasksManager.isBackGroundTaskManagerUP()) {
            return Result.healthy();
        } else {
            return Result.unhealthy("error while checking for background task manager");
        }
    }
}
