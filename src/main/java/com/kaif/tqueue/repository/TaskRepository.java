/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.kaif.tqueue.repository;

import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 *
 * @author kaif
 */
@Repository
public interface TaskRepository extends JpaRepository<Task,Long>{
    public List<Task> findAllTaskByTaskStatus(TaskStatus taskStatus);
    @Query(value = "Select * from task Where task_status='PENDING' limit 1 for update skip locked",nativeQuery=true)
    public Task findPendingWithTaskLocked();
}
