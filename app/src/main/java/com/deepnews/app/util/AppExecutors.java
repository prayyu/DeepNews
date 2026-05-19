package com.deepnews.app.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AppExecutors {

    private static AppExecutors instance;

    private final ExecutorService networkIO;
    private final ExecutorService diskIO;
    private final ExecutorService lightTasks;
    private final ScheduledExecutorService scheduled;

    private AppExecutors() {
        int cores = Runtime.getRuntime().availableProcessors();
        this.networkIO = Executors.newFixedThreadPool(Math.min(cores, 6));
        this.diskIO = Executors.newSingleThreadExecutor();
        this.lightTasks = Executors.newCachedThreadPool();
        this.scheduled = Executors.newSingleThreadScheduledExecutor();
    }

    public static synchronized AppExecutors getInstance() {
        if (instance == null) {
            instance = new AppExecutors();
        }
        return instance;
    }

    public ExecutorService networkIO() { return networkIO; }
    public ExecutorService diskIO() { return diskIO; }
    public ExecutorService lightTasks() { return lightTasks; }
    public ScheduledExecutorService scheduled() { return scheduled; }

    public void shutdown() {
        networkIO.shutdown();
        diskIO.shutdown();
        lightTasks.shutdown();
        scheduled.shutdown();
    }
}
