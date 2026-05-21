package com.kaif.tqueue;

import com.kaif.tqueue.models.Task;
import com.kaif.tqueue.models.TaskStatus;
import com.kaif.tqueue.repository.TaskRepository;
import com.kaif.tqueue.services.TaskService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TqueueApplication  implements CommandLineRunner{

        private final TaskRepository taskRepository;
        private final TaskService taskService;
    
	public static void main(String[] args) {
                System.setProperty("cglib.debugLocation", "./enhanced-classes");
		SpringApplication.run(TqueueApplication.class, args);

	}

        public TqueueApplication(TaskRepository taskRepository,TaskService taskService) {
            this.taskRepository = taskRepository;
            this.taskService = taskService;
        }

        @Override
        public void run(String... args) throws Exception {
//            List<Task> tasksList = new ArrayList<>();
//            for(int i=0;i<1000;i++){
//                  tasksList.
//                          add(Task.builder().name("Task "+(i+1))    
//                          .description("Task "+(i+1))
//                          .taskStatus(TaskStatus.PENDING)
//                          .build()
//                    );
//              }
//            taskRepository.saveAll(tasksList);
//            System.out.println("1000 Tasks inserted");
//            
        }

}
