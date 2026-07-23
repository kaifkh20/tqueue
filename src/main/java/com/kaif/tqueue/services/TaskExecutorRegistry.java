/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.models.Task;
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

    private final Map<String, TaskExecutor> executorMap;

    // Spring automatically discovers and maps all TaskExecutor beans by getTaskType()
    @Autowired
    public TaskExecutorRegistry(List<TaskExecutor> executors) {
        this.executorMap = executors.stream()
                .collect(Collectors.toMap(TaskExecutor::getTaskType, Function.identity()));
    }

    public TaskExecutor resolve(Task task) {
        return executorMap.get(task.getName());
    }
}
