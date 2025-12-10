package com.project.taskscheduler.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "task_execution_history")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@lombok.Builder
public class TaskExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long executionId;

    private Long taskId;
    private String taskName;

    @Enumerated(EnumType.STRING) // Without this, it stores as integer
    private ExecutionStatus status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;

    @Column(length = 1000) // Default is 255, error messages can be longer
    private String error;

    @Column(columnDefinition = "TEXT") // Stack traces are very long
    private String errorStackTrace;

    private String executedBy;
}
