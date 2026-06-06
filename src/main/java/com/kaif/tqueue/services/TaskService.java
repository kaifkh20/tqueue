    /*
     * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
     * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
     */
    package com.kaif.tqueue.services;

import com.kaif.tqueue.models.Task;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

    /**
     *
     * @author kaif
     */

    /*
            Learning :- Save or saveAndFlush doesn't commit but save() saves the context in persistance context 
                        saveAndFlush writes the sql statement in the DB temporary buffer storage(which the database knows about for current transaction but is not visible to other users) but wait's for the commit command
                        which usually happens at then end of method if marked as @Transactional.

    */
    @Service
    public class TaskService {
        private final TaskWorkerService taskWorkerService ;
        private final ThreadPoolTaskExecutor executorPool;
        private final ThreadPoolTaskScheduler taskScheduler;

    public TaskService(TaskWorkerService taskWorkerService,@Qualifier("executorPool")Executor executorPool,ThreadPoolTaskScheduler taskScheduler) {
        this.taskWorkerService = taskWorkerService;
        this.executorPool = (ThreadPoolTaskExecutor)executorPool;
        this.taskScheduler = taskScheduler;
    }   
    
    @EventListener(ApplicationReadyEvent.class)
    public void startWorkerLoop(){
        taskScheduler.schedule(this::processOneTask, Instant.now().plusSeconds(20));
    }
    

//    @Scheduled(fixedRate=2000,initialDelay=20000)
    public void processOneTask(){
//        Task task = taskWorkerService.claimTask();
//        if(task==null){
//            return ;
//        }
//        try{
//            taskWorkerService.executeTask(task);
//        }catch(TaskRejectedException e){
//            System.out.printf("\n[EXECUTOR EXHAUSTED] Worker crashed during execution for Task with ID: %d\n", task.getId());
////            when Task is rejected we mark it as pending after claiming
//            taskWorkerService.pendingTask(task);
//        }
//        catch(Exception e){
//            System.out.printf("\n[EXECPTION] %s",e.getMessage());
//        }

        long nextDelayMs = 2000; // Default base delay (2 seconds)
        boolean taskFound = false;
        Task task = null;
        try {
            task = taskWorkerService.claimTask();
            if (task != null) {
                taskFound = true;
                taskWorkerService.executeTask(task);
            }
        } catch (TaskRejectedException e) {
            // Treat as "task found" but worker pool full, recover state
            taskFound = true; 
            System.out.printf("\n[EXECUTOR EXHAUSTED] Worker crashed during execution for Task with ID: %d\n", task.getId());
            taskWorkerService.pendingTask(task);
        } catch (Exception e) {
            System.out.printf("\n[EXCEPTION] %s", e.getMessage());
        } finally {
            // Calculate delay for the next execution loop
            if (!taskFound) {
                // If queue is empty, add a random jitter between 0 and 3000ms onto the base delay
                long jitter = ThreadLocalRandom.current().nextLong(0, 3000);
                nextDelayMs += jitter;
                System.out.printf("\n[QUEUE EMPTY] Backing off. Next poll in %d ms\n", nextDelayMs);
            }

            // Programmatically schedule the next single execution pass
            taskScheduler.schedule(this::processOneTask, Instant.now().plusMillis(nextDelayMs));
        }
        


        System.out.printf("""

            ================== [EXECUTOR METRICS] ==================
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
