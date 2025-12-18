package com.project.taskscheduler.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Tasks")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskId;

    @Column(nullable = false)
    private String name; 

    @Column(nullable = false)
    private String cronExpression; 

    @Column(nullable = false)
    private String jobClass; 

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true; 

    @Enumerated(EnumType.STRING)    
    @Column(nullable = false)
    private RetryStrategy retryStrategy = RetryStrategy.NONE;

    @Column(nullable = false)
    private Integer maxRetryCount = 3;      //max number of retry attempts

    @Column(nullable = false)
    private Integer retryDelay = 5;         //delay between retry attempts

    @Column(nullable = false)
    private Integer retry_exponentialDelay = 5; //exponential delay between retry attempts
}