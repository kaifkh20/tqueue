/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.ToString;

/**
 *
 * @author kaif
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class Task {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
//    @Column
    private String name;
    
    @Column
    private String description;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus taskStatus;
    
    @Column(columnDefinition = "timestamp with time zone")
    private Instant processingStartedAt;
    
    @Column(columnDefinition = "timestamp with time zone")
    private Instant processingEndedAt;
    
    @Column(columnDefinition = "timestamp with time zone")
    private Instant lastHeartBeatAt;
    
    @Column(columnDefinition = "timestamp with time zone")
    private Instant createdAt;
    
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(columnDefinition = "timestamp with time zone")
    private Instant retriedAt;
    
    @Column(columnDefinition = "timestamp with time zone")
    private Instant nextRetryAt;
    
    @Column(columnDefinition = "timestamp with time zone")
    private Instant heartBeatAt;
    
    @ManyToOne
    @JoinColumn(name="process_execution_id",referencedColumnName="id")
    private ProcessExecution processExecution;
    
    @ManyToOne
    @JoinColumn(name="step_definition_id",referencedColumnName="id")
    private ProcessStepDefinition processStepDefinition;
}
