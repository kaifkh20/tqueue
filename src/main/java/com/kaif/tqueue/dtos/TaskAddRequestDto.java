/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kaif.tqueue.dtos;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author kaif
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAddRequestDto {
     private String taskName;
     private String taskDescription;
     private Long taskDuration;
}
