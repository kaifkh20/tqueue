/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.miscServices.EmailService;
import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author kaif
 */
@Component
public class EmailTaskExecutor implements TaskExecutor{

    @Autowired
    private EmailService emailService;
    
    @Override
    public TaskType getTaskType() {
        return TaskType.EMAIL_SERVICE;
    }

    @Override
    public void execute(Task task) {
        System.out.println("[EMAIL] Executing Email Service...");
        emailService.processEmail(task.getId(),"example@gmail.com");
        System.out.println("[END-EMAIL] Execution of Email Service ended...");
    }
    
}
