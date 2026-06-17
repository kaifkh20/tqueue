/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.dtos.MetricsDto;
import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskStatus;
import com.kaif.tqueue.repository.TaskRepository;
import java.util.List;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 *
 * @author kaif
 */
@Service
public class GeneralService {
    
    private final ThreadPoolTaskExecutor executorPool;
    private final TaskRepository taskRepository;
    
    public GeneralService(@Qualifier("executorPool")Executor executorPool,TaskRepository taskRepository) {
        this.executorPool = (ThreadPoolTaskExecutor)executorPool;
        this.taskRepository = taskRepository;
    }   
    public String getHealthService(){
        return "Healthy and running";
    }
    public MetricsDto getMetrics(){
        Integer corePoolSize = executorPool.getCorePoolSize();
        Integer maxPoolSize = executorPool.getMaxPoolSize();
        Integer activeCount = executorPool.getActiveCount();
        Integer poolSize = executorPool.getPoolSize();
        Integer queueSize = executorPool.getThreadPoolExecutor().getQueue().size();
        Integer remainingQueueSize = executorPool.getThreadPoolExecutor().getQueue().remainingCapacity();
        Long taskCount = executorPool.getThreadPoolExecutor().getTaskCount();
        Long completedTaskCount = executorPool.getThreadPoolExecutor().getCompletedTaskCount();
        
        List<Task> pendingTasks = taskRepository.findAllByTaskStatus(TaskStatus.PENDING);
        List<Task> processingTasks = taskRepository.findAllByTaskStatus(TaskStatus.PROCESSING);
        List<Task> completedTasks = taskRepository.findAllByTaskStatus(TaskStatus.COMPLETED);
        List<Task> failedTasks = taskRepository.findAllByTaskStatus(TaskStatus.FAILED);
        
        MetricsDto response = MetricsDto.builder().
                queue(MetricsDto.Queue.builder().
                        pending(pendingTasks.size())
                        .processing(processingTasks.size())
                        .completed(completedTasks.size())
                        .failed(failedTasks.size()).build()
                )
                .workers(MetricsDto.Workers.builder()
                    .active(activeCount)
                    .maxPoolSize(maxPoolSize)
                    .poolSize(poolSize)
                    .queueSize(queueSize)
                    .remainingCapacity(remainingQueueSize)
                    .build()
                )
                .build();
        
        return response;
    }
}
