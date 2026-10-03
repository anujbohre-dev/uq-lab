package com.anujbohre.uqlab.job;

import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Thread pool that runs jobs in the background. Two workers run at once; up to 100 more wait in
 * the queue. A full queue rejects new jobs instead of exhausting memory.
 */
@Configuration
public class JobExecutorConfig {

    static final int WORKERS = 2;
    static final int QUEUE_CAPACITY = 100;

    // destroyMethod: Spring calls shutdown() when the application stops.
    @Bean(destroyMethod = "shutdown")
    public ExecutorService jobExecutor() {
        AtomicInteger counter = new AtomicInteger();
        return new ThreadPoolExecutor(
                WORKERS,
                WORKERS,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                runnable -> {
                    Thread thread = new Thread(runnable, "uqlab-job-" + counter.incrementAndGet());
                    thread.setDaemon(true); // never keeps the JVM alive on its own
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
