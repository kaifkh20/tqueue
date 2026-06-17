package com.kaif.tqueue.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kaif
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDto {

    private Queue queue;
    private Workers workers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Queue {
        private int pending;
        private int processing;
        private int completed;
        private int failed;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Workers {
        private int active;
        private int poolSize;
        private int maxPoolSize;
        private int queueSize;
        private int remainingCapacity;
    }
    
}