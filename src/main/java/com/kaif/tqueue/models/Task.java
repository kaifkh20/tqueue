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
    
    @Column
    private String name;
    
    @Column
    private String description;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus taskStatus;
    
    private Long taskDuration;
    
    @Column(columnDefinition = "timestamp with time zone")
    private Instant processingStartedAt;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant processingEndedAt;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant lastHeartBeatAt;
    @Column(columnDefinition = "timestamp with time zone")
    private Instant createdAt;
}
