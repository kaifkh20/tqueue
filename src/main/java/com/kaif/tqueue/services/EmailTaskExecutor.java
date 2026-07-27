/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.miscServices.EmailService;
import com.kaif.tqueue.models.ActivityType;
import com.kaif.tqueue.models.Task;
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
    public ActivityType getTaskType() {
        return ActivityType.EMAIL_SERVICE;
    }

    @Override
    public void execute(Task task) {
        System.out.println("[EMAIL] Executing Email Service...");
        emailService.processEmail(task.getId(),"example@gmail.com");
        System.out.println("[END-EMAIL] Execution of Email Service ended...");
    }
    
}
