/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskStatus;
import com.kaif.tqueue.repository.TaskRepository;
import java.time.Duration;
import java.time.Instant;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author kaif
 */
@Service
public class TaskRegistry {
    public final Integer MAX_RETRY  = 5;
    private final TaskRepository taskRepository;
    public TaskRegistry(TaskRepository taskRepository){
        this.taskRepository = taskRepository;
    }
    @Transactional
    public void setProcessingStatus(Task task){
        task.setTaskStatus(TaskStatus.PROCESSING);
        task.setProcessingStartedAt(Instant.now());
        taskRepository.save(task);
    }
    @Transactional
    public void setCompleteStatus(Task task){
        task.setTaskStatus(TaskStatus.COMPLETED);
        task.setProcessingEndedAt(Instant.now());
        taskRepository.save(task);
    }
    @Transactional
    public void setInterruptStatus(Task task){
        task.setTaskStatus(TaskStatus.INTERRUPTED);
        task.setProcessingEndedAt(Instant.now());
        taskRepository.save(task);
    }
    @Transactional
    public void setRetryStatus(Task task) {
        int currentRetries = task.getRetryCount();

        if (currentRetries < MAX_RETRY) {
            // --- TASK CAN BE RETRIED ---
            task.setTaskStatus(TaskStatus.PENDING);
            task.setRetryCount(currentRetries + 1);
            task.setRetriedAt(Instant.now());

            // Exponential backoff using pre-increment retry count: 2^0=1s, 2^1=2s, 2^2=4s...
            long delaySeconds = (long) Math.pow(2, currentRetries);
            long jitterMillis = (long) (Math.random() * 1000);

            task.setNextRetryAt(Instant.now()
                    .plus(Duration.ofSeconds(delaySeconds))
                    .plus(Duration.ofMillis(jitterMillis)));

        } else {
            // --- PERMANENT FAILURE ---
            task.setTaskStatus(TaskStatus.FAILED);
            task.setProcessingEndedAt(Instant.now());
            task.setNextRetryAt(null); // Explicitly clear retry timer
        }

        // Reset runtime tracking fields for the next pickup or terminal state
        task.setHeartBeatAt(null);
        task.setLastHeartBeatAt(null);
        task.setProcessingStartedAt(null);

        taskRepository.save(task);
    }
    
    @Transactional 
    public Task setHeartBeatAt(Task task){
        Instant now = Instant.now();
        task.setHeartBeatAt(now);
        task.setLastHeartBeatAt(now); // Keeping last heartbeat synchronized
        return taskRepository.save(task);
    }

    void setPendingStatus(Task task) {
        task.setTaskStatus(TaskStatus.PENDING);
        task.setNextRetryAt(Instant.now().plus(Duration.ofSeconds(10)));
        taskRepository.save(task);
    }
}
