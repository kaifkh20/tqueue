/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskStatus;
import com.kaif.tqueue.repository.TaskRepository;
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
    public void setRetryStatus(Task task){
        int retryCount = task.getRetryCount();
        if(retryCount<MAX_RETRY){
            task.setTaskStatus(TaskStatus.PENDING);
        }else{
            task.setTaskStatus(TaskStatus.FAILED);   
        }
        task.setRetryCount(retryCount+1);
        task.setRetriedAt(Instant.now());
        taskRepository.save(task);
    }
}
