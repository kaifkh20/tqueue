/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.kaif.tqueue.services;

import com.kaif.tqueue.models.ActivityType;
import com.kaif.tqueue.models.Task;

/**
 *
 * @author kaif
 */



public interface TaskExecutor {
    ActivityType getTaskType();
    void execute(Task task);
}
