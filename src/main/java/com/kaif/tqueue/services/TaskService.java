package com.kaif.tqueue.services;

import com.kaif.tqueue.models.Task;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

/**
 * @author kaif
 *
 * CHANGE FROM PREVIOUS VERSION:
 * Instead of a single sequential poller claiming one task, waiting a fixed
 * 2000ms, then repeating — we now run N independent poller loops (one per
 * core pool thread). Each loop:
 *   - claims a task and submits it for execution
 *   - if it found work, immediately tries to claim again (no artificial delay)
 *   - if the queue was empty, backs off with jitter before trying again
 *
 * This means the pool actually gets saturated with concurrent claims instead
 * of being fed one task every ~2s+ by a single loop.
 */
@Service
public class TaskService {

    private static final long EMPTY_QUEUE_BASE_DELAY_MS = 2000;
    private static final long EMPTY_QUEUE_MAX_JITTER_MS = 3000;
    private static final long BUSY_LOOP_DELAY_MS = 10; // tiny gap to avoid a true hot spin

    private final TaskWorkerService taskWorkerService;
    private final ThreadPoolTaskExecutor executorPool;
    private final ThreadPoolTaskScheduler taskScheduler;

    public TaskService(TaskWorkerService taskWorkerService,
                        @Qualifier("executorPool") Executor executorPool,
                        ThreadPoolTaskScheduler taskScheduler) {
        this.taskWorkerService = taskWorkerService;
        this.executorPool = (ThreadPoolTaskExecutor) executorPool;
        this.taskScheduler = taskScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWorkerLoop() {
        int numPollers = executorPool.getCorePoolSize();
        System.out.printf("\n[STARTUP] Launching %d independent poller loops%n", numPollers);

        for (int workerId = 0; workerId < numPollers; workerId++) {
            // Stagger initial starts slightly so all pollers don't hit the DB
            // in the exact same millisecond on boot.
            long initialDelayMs = 20_000 + (workerId * 50L);
            final int id = workerId;
            taskScheduler.schedule(
                () -> pollLoop(id),
                Instant.now().plusMillis(initialDelayMs)
            );
        }
    }

    /**
     * One independent poller's loop body. Each poller reschedules ITSELF,
     * so there are `numPollers` of these running concurrently and
     * independently — not one loop feeding the whole pool.
     */
    private void pollLoop(int workerId) {
        boolean taskFound = false;
        Task task = null;

        try {
            task = taskWorkerService.claimTask();
            if (task != null) {
                taskFound = true;
                taskWorkerService.executeTask(task);
            }
        } catch (TaskRejectedException e) {
            // Pool was full at submit time — put the claimed task back to PENDING
            // so another poller (or this one, later) can pick it up.
            taskFound = true;
            System.out.printf("\n[EXECUTOR EXHAUSTED][worker-%d] Rejected Task ID: %d%n",
                    workerId, task != null ? task.getId() : -1);
            if (task != null) {
                taskWorkerService.pendingTask(task);
            }
        } catch (Exception e) {
            System.out.printf("\n[EXCEPTION][worker-%d] %s%n", workerId, e.getMessage());
        } finally {
            long nextDelayMs;
            if (taskFound) {
                // There was work — go again almost immediately.
                nextDelayMs = BUSY_LOOP_DELAY_MS;
            } else {
                // Queue looked empty to this poller — back off with jitter
                // so idle pollers aren't hammering the DB in lockstep.
                long jitter = ThreadLocalRandom.current().nextLong(0, EMPTY_QUEUE_MAX_JITTER_MS);
                nextDelayMs = EMPTY_QUEUE_BASE_DELAY_MS + jitter;
            }
            taskScheduler.schedule(() -> pollLoop(workerId), Instant.now().plusMillis(nextDelayMs));
        }

        logMetrics(workerId);
    }

    private void logMetrics(int workerId) {
        System.out.printf("""

            ================== [EXECUTOR METRICS][worker-%d] ==================
            Core Pool Size     : %d
            Max Pool Size      : %d
            Active Threads     : %d (Threads currently running tasks)
            Current Pool Size  : %d (Total physical threads created)
            --------------------------------------------------------
            Tasks in Queue     : %d / 25
            Queue Remaining    : %d
            --------------------------------------------------------
            Total Submitted    : %d (All-time tasks received)
            Total Completed    : %d (Successfully finished)
            ========================================================
            """,
            workerId,
            executorPool.getCorePoolSize(),
            executorPool.getMaxPoolSize(),
            executorPool.getActiveCount(),
            executorPool.getPoolSize(),
            executorPool.getThreadPoolExecutor().getQueue().size(),
            executorPool.getThreadPoolExecutor().getQueue().remainingCapacity(),
            executorPool.getThreadPoolExecutor().getTaskCount(),
            executorPool.getThreadPoolExecutor().getCompletedTaskCount()
        );
    }
}