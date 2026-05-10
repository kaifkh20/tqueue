/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.dtos.TaskAddRequestDto;
import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskStatus;
import com.kaif.tqueue.repository.TaskRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 *
 * @author kaif
 */
@Service
public class TaskWorkerService {
    private final TaskRepository taskRepository;
    
        public TaskWorkerService(TaskRepository taskRepository){
            this.taskRepository = taskRepository;
        }
        public String addTask(TaskAddRequestDto taskAddRequest){
            Task task = Task.builder().name(taskAddRequest.getTaskName()).
                    description(taskAddRequest.getTaskDescription())
                    .taskStatus(TaskStatus.PENDING)
                    .build();
            taskRepository.save(task);

            return "Task succesfully added to the Database";
        }   
        
        @Transactional
        public Task claimTask(){
            Task pendingTask = taskRepository.findPendingWithTaskLocked();
            pendingTask.setTaskStatus(TaskStatus.PROCESSING);
            pendingTask.setProcessingStartedAt(LocalDateTime.now());
            System.out.printf("Task with Id: %d and Name: %s is being processed and being marked as PROCESSING\n",pendingTask.getId(),pendingTask.getName());
            return pendingTask;
        }

        public void executeTask(Task task) throws InterruptedException{
            System.out.printf("Task with ID: %d executor started and is being executed now.\n",task.getId());
            Thread.sleep(10000);
        }

        @Transactional
        public void completeTask(Task task){
            task.setTaskStatus(TaskStatus.COMPLETED);
            task.setProcessingEndedAt(LocalDateTime.now());
            System.out.printf("Task with ID: %d is completed", task.getId());
        }
}
