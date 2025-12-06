package com.project.taskscheduler.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import com.project.taskscheduler.model.TaskExecutionHistory;
import com.project.taskscheduler.model.ExecutionStaus;

public interface TaskExecutionHistoryRepository {
    Page<TaskExecutionHistory> findByTaskIdOrderByStartTimeDesc(Long taskId, Pageable pageable);
    List<TaskExecutionHistory> findByStatus(ExecutionStaus status);
    List<TaskExecutionHistory> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    List<TaskExecutionHistory> findByTaskId(Long taskId);
}
