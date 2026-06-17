package com.kaif.tqueue.miscServices;

import com.kaif.tqueue.models.EmailServiceModel;
import com.kaif.tqueue.repository.EmailServiceRepository;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailService {
    
    private final EmailServiceRepository emailServiceRepository;
    
    public EmailService(EmailServiceRepository emailServiceRepository){
        this.emailServiceRepository = emailServiceRepository;
    }
    
    @Transactional
    public void processEmail(Long taskId, String recipient){
        try {
            EmailServiceModel emailServiceModel = EmailServiceModel.builder()
                    .taskId(taskId)
                    .recipient(recipient)
                    .sentAt(Instant.now())
                    .build();
            
            emailServiceRepository.saveAndFlush(emailServiceModel);
            
            executeEmailDelivery(taskId, recipient);
            
        } catch (DataIntegrityViolationException e) {
            System.out.printf("Duplicate request caught for Task ID: %d. Ignoring to guarantee idempotency.\n", taskId);
        }
    }
    
    private void executeEmailDelivery(Long taskId, String recipient) {
        System.out.printf("Task ID: %d successfully sent to Email: %s\n", taskId, recipient);
    }
}