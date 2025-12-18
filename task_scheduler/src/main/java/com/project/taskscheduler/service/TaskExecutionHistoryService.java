package com.project.taskscheduler.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.taskscheduler.model.ExecutionStatus;
import com.project.taskscheduler.model.TaskExecutionHistory;
import com.project.taskscheduler.repository.TaskExecutionHistoryRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Service
public class TaskExecutionHistoryService {

    @Autowired
    private TaskExecutionHistoryRepository taskExecutionHistoryRepository;

    public Long recordExecutionStart(Long taskId, String taskName) {
        String hostname = getHostname();

        TaskExecutionHistory execution = TaskExecutionHistory.builder()
                .taskId(taskId)
                .taskName(taskName)
                .status(ExecutionStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .executedBy(hostname)
                .build();

        TaskExecutionHistory saved = taskExecutionHistoryRepository.save(execution);
        return saved.getExecutionId();
    }

    public Long recordRetryExecutionStart(Long taskId, String taskName, int retryAttempt, Long parentExecutionId) {
        String hostname = getHostname();

        TaskExecutionHistory execution = TaskExecutionHistory.builder()
                .taskId(taskId)
                .taskName(taskName)
                .status(ExecutionStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .executedBy(hostname)
                .retryAttempts(retryAttempt)
                .parentExecutionId(parentExecutionId)
                .isRetry(true)
                .build();

        TaskExecutionHistory saved = taskExecutionHistoryRepository.save(execution);
        return saved.getExecutionId();
    }

    public void recordExecutionSuccess(Long executionId) {
        Optional<TaskExecutionHistory> optionalExecution = taskExecutionHistoryRepository.findById(executionId);
        if (optionalExecution.isPresent()) {
            TaskExecutionHistory execution = optionalExecution.get();
            LocalDateTime endTime = LocalDateTime.now();

            execution.setStatus(ExecutionStatus.SUCCESS);
            execution.setEndTime(endTime);
            execution.setDuration(calculateDuration(execution.getStartTime(), endTime));

            taskExecutionHistoryRepository.save(execution);
        }
    }

    public TaskExecutionHistory recordExecutionFailure(Long executionId, Exception error) {
        Optional<TaskExecutionHistory> optionalExecution = taskExecutionHistoryRepository.findById(executionId);
        if (optionalExecution.isPresent()) {
            TaskExecutionHistory execution = optionalExecution.get();
            LocalDateTime endTime = LocalDateTime.now();

            execution.setStatus(ExecutionStatus.FAILURE);
            execution.setEndTime(endTime);
            execution.setDuration(calculateDuration(execution.getStartTime(), endTime));
            execution.setError(error.getMessage());
            execution.setErrorStackTrace(getStackTraceAsString(error));

            return taskExecutionHistoryRepository.save(execution);
        }
        return null;
    }

    public Page<TaskExecutionHistory> getTaskExecutionHistory(Long taskId, Pageable pageable) {
        return taskExecutionHistoryRepository.findByTaskIdOrderByStartTimeDesc(taskId, pageable);
    }

    public List<TaskExecutionHistory> getTaskExecutionHistoryByStatus(ExecutionStatus status) {
        return taskExecutionHistoryRepository.findByStatus(status);
    }

    public List<TaskExecutionHistory> getTaskExecutionHistoryByTime(LocalDateTime starTime, LocalDateTime endTime) {
        return taskExecutionHistoryRepository.findByStartTimeBetween(starTime, endTime);
    }

    public TaskExecutionHistory getExecutionById(Long executionId) {
        return taskExecutionHistoryRepository.findById(executionId).orElse(null);
    }

    public List<TaskExecutionHistory> getRetryHistory(Long parentExecutionId) {
        return taskExecutionHistoryRepository.findByParentExecutionId(parentExecutionId);
    }

    public List<TaskExecutionHistory> getRetryHistoryForTask(Long taskId) {
        return taskExecutionHistoryRepository.findByTaskIdAndIsRetry(taskId, true);
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Long calculateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        return Duration.between(startTime, endTime).getSeconds();
    }

    private String getStackTraceAsString(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public ExecutionStatistics getExecutionStatistics(Long taskId) {
        long totalExecutions = taskExecutionHistoryRepository.countByTaskId(taskId);
        long successCount = taskExecutionHistoryRepository.countByTaskIdAndStatus(taskId, ExecutionStatus.SUCCESS);
        long failureCount = taskExecutionHistoryRepository.countByTaskIdAndStatus(taskId, ExecutionStatus.FAILURE);

        List<TaskExecutionHistory> executions = taskExecutionHistoryRepository.findByTaskId(taskId);
        double avgDuration = executions.stream()
                .filter(e -> e.getDuration() != null)
                .mapToLong(TaskExecutionHistory::getDuration)
                .average()
                .orElse(0.0);

        return new ExecutionStatistics(totalExecutions, successCount, failureCount, avgDuration);
    }

    @Getter
    @AllArgsConstructor
    public static class ExecutionStatistics {
        private final long totalExecutions;
        private final long successCount;
        private final long failureCount;
        private final double averageDuration;
    }
}
