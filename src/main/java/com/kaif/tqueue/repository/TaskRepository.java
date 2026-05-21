/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.kaif.tqueue.repository;

import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskStatus;
import java.util.List;
import java.util.Optional;
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
//    @Query(value = "Select * from task Where task_status='PENDING' limit 1 for update skip locked",nativeQuery=true)
//    public Task findPendingWithTaskLocked();
//    @Query(value="Select * from task where task status='PROCESSING' and processing_started_at-now()>INTERVAL '5 minutes' limit 1 for update skip locked",nativeQuery=true)
//    public Task findTaskProcessingForMoreThanFiveMinutes();
    @Query(value = """
        SELECT *
        FROM task
        WHERE (
            task_status = 'PENDING'
            OR (
                task_status = 'PROCESSING'
                AND processing_started_at < NOW() - INTERVAL '5 minutes'
            )
        )
        ORDER BY created_at
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true)
    Optional<Task> findNextTaskToProcess();


}
