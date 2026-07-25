/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author kaif
 */
@Service
public class TaskExecutorRegistry {

    private final Map<TaskType, TaskExecutor> executorMap;

    @Autowired
    public TaskExecutorRegistry(List<TaskExecutor> executors) {
        this.executorMap = executors.stream()
                .collect(Collectors.toMap(
                    TaskExecutor::getTaskType, 
                    Function.identity(),
                    (existing, replacement) -> existing // Prevents crashes if two beans register the same type
                ));
    }

    public TaskExecutor resolve(Task task) {
        TaskExecutor executor = executorMap.get(task.getTaskType());        
        if (executor == null) {
            throw new IllegalArgumentException("No executor registered for task type: " + task.getTaskType());
        }
        
        return executor;
    }
}
