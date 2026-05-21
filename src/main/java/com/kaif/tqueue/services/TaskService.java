    /*
     * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
     * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
     */
    package com.kaif.tqueue.services;

    import com.kaif.tqueue.models.Task;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Service;

    /**
     *
     * @author kaif
     */

    /*
            Learning :- Save or saveAndFlush doesn't commit but save() saves the context in persistance context 
                        saveAndFlush writes the sql statement in the DB temporary buffer storage(which the database knows about for current transaction but is not visible to other users) but wait's for the commit command
                        which usually happens at then end of method if marked as @Transactional.

    */
    @Service
    public class TaskService {
        private final TaskWorkerService taskWorkerService ;

    public TaskService(TaskWorkerService taskWorkerService) {
        this.taskWorkerService = taskWorkerService;
    }

    @Scheduled(fixedRate=2000,initialDelay=20000)
    public void processOneTask(){
        Task task = taskWorkerService.claimTask();
        if(task==null){
            return ;
        }
        try{
            taskWorkerService.executeTask(task);
        }catch(Exception e){
            System.out.printf("Worker crashed for Task with ID: %d\n",task.getId());
        }
    }
    }
