package com.project.taskscheduler.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.taskscheduler.model.RetryStrategy;
import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.model.TaskExecutionHistory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RetryService {

    @Autowired
    private Scheduler scheduler;

    public boolean shouldRetry(Task task, TaskExecutionHistory execution) {
        if (task.getRetryStrategy() == RetryStrategy.NONE) {
            log.info("Task has retry strategy NONE, skipping retry", task.getTaskId());
            return false;
        }

        int currentAttempt = execution.getRetryAttempts() != null ? execution.getRetryAttempts() : 0;

        if (currentAttempt >= task.getMaxRetryCount()) {
            log.info("Task has reached max retry attempts, skipping retry", task.getTaskId());
            return false;
        }
        return true;
    }

    public long calculateRetryDelay(Task task, int retryAttempt) {
        switch (task.getRetryStrategy()) {
            case IMMEDIATE:
                return 0;
            case FIXED_DELAY:
                return task.getRetryDelay();
            case EXPONENTIAL_DELAY:
                int initialDelay = task.getRetryDelay();
                long delay = (long) (initialDelay * Math.pow(2, retryAttempt));
                long maxDelay = 300;
                return Math.min(delay, maxDelay);
            default:
                return 0;
        }
    }

    public void scheduleRetry(Task task, TaskExecutionHistory failedExecution, Exception error) {
        try {
            System.out.println("DEBUG: Entering scheduleRetry for task " + task.getTaskId());
            if (!shouldRetry(task, failedExecution)) {
                System.out.println("DEBUG: shouldRetry returned false.");
                return;
            }
            System.out.println("DEBUG: shouldRetry returned true. Scheduling...");
            int nextAttempt = (failedExecution.getRetryAttempts() != null ? failedExecution.getRetryAttempts() : 0) + 1;
            long delaySeconds = calculateRetryDelay(task, nextAttempt);

            LocalDateTime startTime = LocalDateTime.now().plusSeconds(delaySeconds);
            Date startDate = Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());

            Class<? extends Job> jobClass = loadJobClass(task.getJobClass());

            String retryJobName = "RetryJob_" + task.getTaskId() + nextAttempt;

            JobDataMap dataMap = new JobDataMap();
            dataMap.put("taskID", task.getTaskId());
            dataMap.put("retryAttempt", nextAttempt);
            dataMap.put("parentExecutionId", failedExecution.getExecutionId());
            dataMap.put("error", error.getMessage());
            dataMap.put("isRetry", true);

            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(retryJobName, "retry")
                    .usingJobData(dataMap)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(retryJobName, "retry")
                    .startAt(startDate)
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled retry for task {} (attempt {}/{}) in {} seconds at {}",
                    task.getTaskId(), nextAttempt, task.getMaxRetryCount(), delaySeconds, startTime);
        } catch (Exception e) {
            log.error("Failed to schedule retry for task {}", task.getTaskId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Job> loadJobClass(String className) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        if (!Job.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class " + className + " does not implement org.quartz.Job");
        }
        return (Class<? extends Job>) clazz;
    }

}
