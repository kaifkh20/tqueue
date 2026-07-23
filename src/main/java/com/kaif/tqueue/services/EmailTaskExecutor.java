/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.miscServices.EmailService;
import com.kaif.tqueue.models.Task;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author kaif
 */
public class EmailTaskExecutor implements TaskExecutor{

    @Autowired
    private EmailService emailService;
    
    @Override
    public String getTaskType() {
        return "EMAIL_SERVICE";
    }

    @Override
    public void execute(Task task) {
        emailService.processEmail(task.getId(),"example@gmail.com");
    }
    
}
