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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private final Exchanger<String> exchanger;
    
        public TaskWorkerService(TaskRepository taskRepository,TaskRegistry taskRegistry){
            this.taskRepository = taskRepository;
            this.taskRegistry = taskRegistry;
            this.exchanger = new Exchanger<>();
        }
        public String addTask(TaskAddRequestDto taskAddRequest){
            Task task = Task.builder().name(taskAddRequest.getTaskName()).
                    description(taskAddRequest.getTaskDescription())
                    .taskStatus(TaskStatus.PENDING)
                    .taskDuration(taskAddRequest.getTaskDuration())
                    .createdAt(Instant.now())
                    .shouldFail(taskAddRequest.isShouldFail())
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
            System.out.printf("Task with Id: %d and Name: %s is claimed and will be marked as PROCESSING\n",pendingTask.get().getId(),pendingTask.get().getName());
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
               System.out.printf("\n[WORKING]Task with ID: %d and Name: %s will be working and using the thread for %d seconds\n",task.getId(),task.getName(),task.getTaskDuration());
//               Thread.sleep(task.getTaskDuration()*1000);//s->ms
                long durationMs = task.getTaskDuration() * 1000L;
                long startTime = System.currentTimeMillis();
                long endTime = startTime + durationMs;
                
                // when the task starts we heartbeat it 
                updateHeartBeat(task);
                
                while (System.currentTimeMillis() < endTime) {

                    if (Thread.currentThread().isInterrupted()) {
                        System.out.printf("\n[CANCELLED] Task with ID: %d was interrupted.\n", task.getId());
                        throw new InterruptedException("Task aborted due to executor shutdown.");
                    }
                    
                    Instant heartBeatAt = task.getHeartBeatAt();
                    // every 10 seconds we check if its healthy
                    if(Instant.now().isAfter(heartBeatAt.plusSeconds(10))){
                        updateHeartBeat(task);
                    }

                    Thread.onSpinWait(); 
                }
               if(task.isShouldFail()){
                   throw new RuntimeException("Simulated Failure");
               }
               System.out.printf("\n[WORKED]Task with ID: %d and Name: %s worked and used the thread for %d seconds\n",task.getId(),task.getName(),task.getTaskDuration());
               completeTask(task);
           }catch(InterruptedException e){
                System.out.printf("\n[SHUTDOWN] Task interrupted for ID: %d. Skipping completion.\n", task.getId());
                interruptTask(task);
                Thread.currentThread().interrupt();
            }catch(RuntimeException e){
                System.out.printf("\n[RUNTIME EXCEPTION] Task ID: %d. Retry Count: %d \n",task.getId(),task.getRetryCount());
                retryTask(task);
            }catch (Exception e) {
                System.out.printf("\n[WORKER CRASHED] Worker crashed during execution for Task with ID: %d\n", task.getId());
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
                System.out.printf("\n[RECOVERY] Initiating auto-retry for Task [ID: %d | Name: %s]\n", 
                        task.getId(), task.getName());

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
        
        public void completeTask(Task task){
            taskRegistry.setCompleteStatus(task);
            System.out.printf("Task with ID: %d is completed\n", task.getId());
        }
}
