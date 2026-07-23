/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.controller;

import com.kaif.tqueue.dtos.TaskAddRequestDto;
import com.kaif.tqueue.services.TaskWorkerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author kaif
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {
    private final TaskWorkerService taskWorkerService;
    
    public TaskController(TaskWorkerService taskWorkerService){
        this.taskWorkerService = taskWorkerService;
    }
    
    @PostMapping("/add")
    public ResponseEntity<?> addTask(@RequestBody TaskAddRequestDto taskAddRequest){
           return ResponseEntity.ok(taskWorkerService.addTask(taskAddRequest));
    }
    
}
