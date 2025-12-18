package com.project.taskscheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.model.TaskExecutionHistory;
import com.project.taskscheduler.repository.TaskRepository;
import com.project.taskscheduler.service.RetryService;
import com.project.taskscheduler.service.TaskExecutionHistoryService;

public class LogTimeJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(LogTimeJob.class);

    @Autowired
    private TaskExecutionHistoryService executionHistoryService;

    @Autowired
    private RetryService retryService;

    @Autowired
    private TaskRepository taskRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = (Long) context.getMergedJobDataMap().get("taskId");
        String taskName = context.getJobDetail().getKey().getName();

        // Check if this is a retry
        Boolean isRetry = (Boolean) context.getMergedJobDataMap().get("isRetry");
        Integer retryAttempt = (Integer) context.getMergedJobDataMap().get("retryAttempt");
        Long parentExecutionId = (Long) context.getMergedJobDataMap().get("parentExecutionId");

        Long executionId = null;

        try {
            // Record execution start
            if (isRetry != null && isRetry) {
                executionId = executionHistoryService.recordRetryExecutionStart(
                        taskId, taskName, retryAttempt, parentExecutionId);
            } else {
                executionId = executionHistoryService.recordExecutionStart(taskId, taskName);
            }

            // Actual job logic
            log.info("[LogTimeJob] Executed at={} (taskId={})", java.time.LocalDateTime.now(), taskId);

            // Record success
            executionHistoryService.recordExecutionSuccess(executionId);

        } catch (Exception e) {
            // Record failure
            if (executionId != null) {
                executionHistoryService.recordExecutionFailure(executionId, e);

                // Schedule retry if configured
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task != null) {
                    TaskExecutionHistory failedExecution = executionHistoryService.getExecutionById(executionId);
                    retryService.scheduleRetry(task, failedExecution, e);
                }
            }
            log.error("[LogTimeJob] Execution failed for taskId={}", taskId, e);
            throw new JobExecutionException(e);
        }
    }
}
