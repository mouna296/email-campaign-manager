package com.untitled.ecm.core;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class DakiyaBackGroundTasksManager implements Managed {
    private static ThreadPoolExecutor dakiyaThreadPoolExecutor;
    private static Logger logger;
    final private int MAX_BACKGROUND_TASKS;
    final private int MIN_BACKGROUND_TASKS;

    public DakiyaBackGroundTasksManager(int maxBackGroundTasks) {
        if (maxBackGroundTasks < 1) {
            this.MAX_BACKGROUND_TASKS = 1;
        } else {
            this.MAX_BACKGROUND_TASKS = maxBackGroundTasks;
        }
        // this is necessary to ensure that we have enough space in jre heap at start of dakiya itself
        if (this.MAX_BACKGROUND_TASKS < 5) {
            this.MIN_BACKGROUND_TASKS = this.MAX_BACKGROUND_TASKS;
        } else {
            this.MIN_BACKGROUND_TASKS = 5;
        }

        logger = LoggerFactory.getLogger(DakiyaBackGroundTasksManager.class);

    }

    /* ===============================================================================
       methods below this make sure that threadpoolexecuter is exposed on need basis
    ==================================================================================*/
    public static boolean submitTask(Runnable runnable) {
        try {
            DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor.submit(runnable);
            return true;
        } catch (Exception e) {
            DakiyaBackGroundTasksManager.logger.error("could not add task: Causing: " + e.getMessage());
            return false;
        }

    }

    public static boolean submitTask(Callable callable) {
        try {
            // we might do something with this future, maybe save it in queue/list
            Future future = DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor.submit(callable);
            return true;
        } catch (Exception e) {
            DakiyaBackGroundTasksManager.logger.error("could not add task: Causing: " + e.getMessage());
            return false;
        }
    }

    static int getCurrentBackGroundThreadCount() {
        return DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor.getPoolSize();
    }

    public static boolean isBackGroundTaskManagerUP() {
        if (DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor == null) {
            return false;
        }
        int currentThreadCount = DakiyaBackGroundTasksManager.getCurrentBackGroundThreadCount();
        int maxThreadCount = DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor.getMaximumPoolSize();
        int minThreadCound = DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor.getCorePoolSize();
        return (currentThreadCount >= minThreadCound || currentThreadCount <= maxThreadCount);
    }

    @Override
    public void start() throws Exception {
        if (this.tasksExecuterExists()) {
            return;
        }
        this.startThreadPoolExecuter();
    }

    @Override
    public void stop() throws Exception {
        if (!this.tasksExecuterExists()) {
            DakiyaBackGroundTasksManager.logger.error("background task does not exist, cannot shut down");
            return;
        }
        int activeTasks = DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor.getActiveCount();
        if (activeTasks > 0) {
            DakiyaBackGroundTasksManager.logger.warn(Integer.toString(activeTasks) + " tasks are executing currently, waiting for them to finish. No new task can be added now onwards");
            try {
                DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor.shutdown();
            } catch (Exception e) {
                DakiyaBackGroundTasksManager.logger.error("could not shutdown background tasks. Causing: " + e.getMessage());
            }
        } else {
            try {
                DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor.shutdownNow();
                DakiyaBackGroundTasksManager.logger.info("stopped all the background tasks");
            } catch (Exception e) {
                DakiyaBackGroundTasksManager.logger.error("could not shutdown background tasks. Causing: " + e.getMessage());
            }
        }
    }

    private void startThreadPoolExecuter() {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        // if a thread is idle for this much time unit, it will be removed
        int keepAliveTime = 500;
        DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor =
                new ThreadPoolExecutor(
                        this.MIN_BACKGROUND_TASKS,
                        this.MAX_BACKGROUND_TASKS,
                        keepAliveTime,
                        timeUnit,
                        new LinkedBlockingQueue<Runnable>());

        int coreThreads = DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor.prestartAllCoreThreads();
        DakiyaBackGroundTasksManager.logger.info(Integer.toString(coreThreads) + " threads of max " +
                Integer.toString(this.MAX_BACKGROUND_TASKS) + " started to handle background tasks");
    }

    private boolean tasksExecuterExists() {
        return (DakiyaBackGroundTasksManager.dakiyaThreadPoolExecutor != null);
    }


}
