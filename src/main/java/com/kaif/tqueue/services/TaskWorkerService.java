/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.dtos.TaskAddRequestDto;
import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskStatus;
import com.kaif.tqueue.repository.TaskRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.scheduling.annotation.Async;
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
    
        public TaskWorkerService(TaskRepository taskRepository,TaskRegistry taskRegistry){
            this.taskRepository = taskRepository;
            this.taskRegistry = taskRegistry;
        }
        public String addTask(TaskAddRequestDto taskAddRequest){
            Task task = Task.builder().name(taskAddRequest.getTaskName()).
                    description(taskAddRequest.getTaskDescription())
                    .taskStatus(TaskStatus.PENDING)
                    .taskDuration(taskAddRequest.getTaskDuration())
                    .createdAt(Instant.now())
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
            System.out.printf("Task with Id: %d and Name: %s is being processed and being marked as PROCESSING\n",pendingTask.get().getId(),pendingTask.get().getName());
            return pendingTask.get();
        }

//        public void executeTask(Task task) throws InterruptedException{
//            System.out.printf("Task with ID: %d executor started and is being executed now.\n",task.getId());
//            Thread.sleep(10000);
//            System.out.printf("Task with ID: %d execution completed\n",task.getId());
//        }
        @Async("executorPool")
        public void executeTask(Task task) throws InterruptedException {
           try{
               System.out.printf("Task with ID: %d and Name: %s will be working and using the thread for %d seconds\n",task.getId(),task.getName(),task.getTaskDuration());
               Thread.sleep(task.getTaskDuration()*1000);//s->ms
               System.out.printf("Task with ID: %d and Name: %s worked and used the thread for %d seconds\n",task.getId(),task.getName(),task.getTaskDuration());
               completeTask(task);
           }catch(InterruptedException e){
                System.out.printf("\n[SHUTDOWN] Task interrupted for ID: %d. Skipping completion.\n", task.getId());
                Thread.currentThread().interrupt();
            }catch (Exception e) {
                System.out.printf("Worker crashed during execution for Task with ID: %d\n", task.getId());
            }
        }
        
        public void completeTask(Task task){
            taskRegistry.setCompleteStatus(task);
            System.out.printf("Task with ID: %d is completed\n", task.getId());
        }
}
