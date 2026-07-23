/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.dtos.TaskAddRequestDto;
import com.kaif.tqueue.miscServices.EmailService;
import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskStatus;
import com.kaif.tqueue.repository.TaskRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author kaif
 */



@Service
public class TaskWorkerService {
    
    private final TaskRepository taskRepository;
    private final TaskRegistry taskRegistry;
    private final TaskExecutorRegistry taskExecutorRegistry;
    
        public TaskWorkerService(TaskRepository taskRepository,TaskRegistry taskRegistry,TaskExecutorRegistry taskExecutorRegistry){
            this.taskRepository = taskRepository;
            this.taskRegistry = taskRegistry;
            this.taskExecutorRegistry = taskExecutorRegistry;
        }
        public String addTask(TaskAddRequestDto taskAddRequest){
            Task task = Task.builder()
                    .taskStatus(TaskStatus.PENDING)
//                    .taskDuration(taskAddRequest.getTaskDuration())
                    .createdAt(Instant.now())
//                    .shouldFail(taskAddRequest.isShouldFail())
                    .build();
            taskRepository.save(task);

            return "Task succesfully added to the Database";
        }   
        
        @Transactional
        public Task claimTask(){
            Optional<Task> pendingTask = taskRepository.findNextTaskToProcess();
            System.out.println("PENDING TASK "+pendingTask.toString());
            if(pendingTask.isEmpty()){
                return null;
            }
            pendingTask.get().setTaskStatus(TaskStatus.PROCESSING);
            pendingTask.get().setProcessingStartedAt(Instant.now());
            System.out.printf("Task with Id: %d is claimed and will be marked as PROCESSING\n",pendingTask.get().getId());
            return pendingTask.get();
        }

    @Async("executorPool")
    public void executeTask(Task task) {
        System.out.printf("\n[WORKING] Task with ID: %d \n", task.getId());
        long startTime = System.currentTimeMillis();

        // 1. Spin up a lightweight scheduler dedicated solely to this task's background heartbeat
        ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

        try {
            heartbeatScheduler.scheduleAtFixedRate(() -> {
                try {
                    updateHeartBeat(task);
                } catch (Exception e) {
                    System.err.printf("Failed to update heartbeat for task %d: %s\n", task.getId(), e.getMessage());
                }
            }, 0, 5, TimeUnit.SECONDS);
            
            TaskExecutor executor = taskExecutorRegistry.resolve(task);
            executor.execute(task);

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            System.out.printf("\n[WORKED] Task with ID: %d and completed in %d seconds\n", task.getId(), duration);

            completeTask(task);

        } catch (RuntimeException e) {
            System.out.printf("\n[RUNTIME EXCEPTION] Task ID: %d. Retry Count: %d \n", task.getId(), task.getRetryCount());
            retryTask(task);

        } catch (Exception e) {
            if (e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
                System.out.printf("\n[SHUTDOWN] Task interrupted for ID: %d. Skipping completion.\n", task.getId());
                interruptTask(task);
                Thread.currentThread().interrupt(); 
            } else {
                System.out.printf("\n[WORKER CRASHED] Unexpected failure for Task with ID: %d\n", task.getId());
                e.printStackTrace();
            }
        } finally {
            heartbeatScheduler.shutdown();
        }
    }
        
        
        //    every 30 seconds it checks for dead workers and retry it 
        @Scheduled(fixedRate = 30000)
        public void checkForDeadWorkers() {
            System.out.println("\n[DEAD-MAN-SWITCH] Running periodic sweep for hung or dead tasks...");

            List<Task> deadTasks = taskRepository.getDeadTasks();

            if (deadTasks.isEmpty()) {
                System.out.println("\n[DEAD-MAN-SWITCH] Healthy system. Zero dead tasks detected.");
                return;
            }

            System.out.printf("\n[DEAD-MAN-SWITCH] WARNING: Found %d tasks that missed their heartbeat thresholds.\n", deadTasks.size());

            for (Task task : deadTasks) {
                System.out.printf("\n[RECOVERY] Initiating auto-retry for Task [ID: %d]\n", 
                        task.getId());

                try {
                    retryTask(task);
                    System.out.printf("\n[SUCCESS] Task [ID: %d] successfully re-queued for execution.\n", task.getId());
                } catch (Exception e) {
                    System.err.printf("\n[ERROR] Failed to retry Task [ID: %d]. Reason: %s\n", 
                            task.getId(), e.getMessage());
                }
            }
        }
        
        public void updateHeartBeat(Task task){
            taskRegistry.setHeartBeatAt(task);
            System.out.printf("\n[HEARTBEAT]Task with ID: %d is active Last Heartbeat at: %s\n",task.getId(),task.getHeartBeatAt().toString());
        }
        
        public void processTask(Task task){
            taskRegistry.setProcessingStatus(task);
            System.out.printf("Task with ID: %d is being PROCESSED...\n",task.getId());
        }
        
        public void retryTask(Task task){
            taskRegistry.setRetryStatus(task);
            System.out.printf("Task with ID: %d is being retried...\n", task.getId());
        }
        
        public void interruptTask(Task task){
            taskRegistry.setInterruptStatus(task);
            System.out.printf("Task with ID: %d is interrupted\n",task.getId());
        }
        
        public void pendingTask(Task task){
            taskRegistry.setPendingStatus(task);
            System.out.printf("Task with ID: %d is being marked as pending\n",task.getId());
        }
        
        public void completeTask(Task task){
            taskRegistry.setCompleteStatus(task);
            System.out.printf("Task with ID: %d is completed\n", task.getId());
        }
}
