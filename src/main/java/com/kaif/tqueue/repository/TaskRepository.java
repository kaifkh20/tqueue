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
            -- Condition 1: Ready for a retry (missed heartbeat tasks reset to PENDING)
            (task_status = 'PENDING' AND retry_count <= 5 AND next_retry_at IS NOT NULL AND next_retry_at <= NOW())
            OR 
            -- Condition 2: Brand new pending tasks (never run or retried before)
            (task_status = 'PENDING' AND next_retry_at IS NULL)
        )
        ORDER BY 
            -- Priority 1: Retries come first (assign lowest weight)
            CASE 
                WHEN task_status = 'PENDING' AND next_retry_at IS NOT NULL THEN 1
                ELSE 2 -- Brand new PENDING tasks
            END ASC,
            -- Priority 2: Tie-breaker within those categories (oldest first)
            created_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    """,
    nativeQuery = true)
    Optional<Task> findNextTaskToProcess();
    @Query(value = """
        SELECT * FROM task 
        WHERE task_status = 'PROCESSING' 
          AND heart_beat_at < NOW() - INTERVAL '25 seconds' 
          AND retry_count <= 5 
          AND (next_retry_at IS NULL OR next_retry_at <= NOW())
        ORDER BY created_at ASC 
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<Task> getDeadTasks();    
}
