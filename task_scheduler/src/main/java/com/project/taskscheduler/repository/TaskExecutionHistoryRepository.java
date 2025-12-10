package com.project.taskscheduler.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import com.project.taskscheduler.model.TaskExecutionHistory;
import com.project.taskscheduler.model.ExecutionStatus;

public interface TaskExecutionHistoryRepository extends JpaRepository<TaskExecutionHistory, Long> {
    Page<TaskExecutionHistory> findByTaskIdOrderByStartTimeDesc(Long taskId, Pageable pageable);

    List<TaskExecutionHistory> findByStatus(ExecutionStatus status);

    List<TaskExecutionHistory> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<TaskExecutionHistory> findByTaskId(Long taskId);

    long countByTaskId(Long taskId);

    long countByTaskIdAndStatus(Long taskId, ExecutionStatus status);

}
