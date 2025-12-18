# Task Retry Mechanism - Complete Implementation Code

## 1. UPDATE Task.java - Add @Builder.Default to retry fields
**Location:** `src/main/java/com/project/taskscheduler/model/Task.java`

Replace your retry fields section with this:

```java
@Enumerated(EnumType.STRING)    
@Column(nullable = false)
@Builder.Default
private RetryStrategy retryStrategy = RetryStrategy.NONE;

@Column(nullable = false)
@Builder.Default
private Integer maxRetryCount = 3;

@Column(nullable = false)
@Builder.Default
private Integer retryDelay = 30;  // seconds for FIXED_DELAY

@Column(nullable = false)
@Builder.Default
private Integer retryBackoff = 1;  // initial seconds for EXPONENTIAL_BACKOFF
```

---

## 2. UPDATE TaskExecutionHistory.java - Add @Builder.Default
**Location:** `src/main/java/com/project/taskscheduler/model/TaskExecutionHistory.java`

Update retry fields:

```java
@Column(nullable = false)
@Builder.Default
private Integer retryAttempt = 0;

@Column
private Long parentExecutionId;

@Column(nullable = false)
@Builder.Default
private Boolean isRetry = false;
```

---

## 3. UPDATE TaskExecutionHistoryRepository.java - Add query method
**Location:** `src/main/java/com/project/taskscheduler/repository/TaskExecutionHistoryRepository.java`

Add this method:

```java
List<TaskExecutionHistory> findByParentExecutionId(Long parentExecutionId);
```

---

## 4. CREATE RetryPolicyService.java
**Location:** `src/main/java/com/project/taskscheduler/service/RetryPolicyService.java`

```java
package com.project.taskscheduler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.taskscheduler.model.RetryStrategy;
import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.model.TaskExecutionHistory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RetryPolicyService {

    @Autowired
    private TaskExecutionHistoryService executionHistoryService;

    /**
     * Determine if a failed task should be retried
     */
    public boolean shouldRetry(Task task, TaskExecutionHistory execution) {
        // No retry if strategy is NONE
        if (task.getRetryStrategy() == RetryStrategy.NONE) {
            log.info("Task {} has retry strategy NONE, skipping retry", task.getTaskId());
            return false;
        }

        // Check if max retries exceeded
        int currentAttempt = execution.getRetryAttempt() != null ? execution.getRetryAttempt() : 0;
        if (currentAttempt >= task.getMaxRetryCount()) {
            log.warn("Task {} exceeded max retry attempts ({}/{})", 
                task.getTaskId(), currentAttempt, task.getMaxRetryCount());
            return false;
        }

        log.info("Task {} will be retried. Attempt {}/{}", 
            task.getTaskId(), currentAttempt + 1, task.getMaxRetryCount());
        return true;
    }

    /**
     * Calculate delay in seconds before next retry based on strategy
     */
    public long calculateRetryDelay(Task task, int retryAttempt) {
        switch (task.getRetryStrategy()) {
            case IMMEDIATE:
                return 0;  // No delay

            case FIXED_DELAY:
                return task.getRetryDelay();  // Same delay every time

            case EXPONENTIAL_BACKOFF:
                // Formula: initialDelay * (2 ^ retryAttempt)
                // Retry 1: 1 * 2^0 = 1 second
                // Retry 2: 1 * 2^1 = 2 seconds
                // Retry 3: 1 * 2^2 = 4 seconds
                // Retry 4: 1 * 2^3 = 8 seconds
                int initialDelay = task.getRetryBackoff();
                long delay = (long) (initialDelay * Math.pow(2, retryAttempt));
                
                // Cap at 5 minutes to prevent extremely long delays
                long maxDelay = 300; // 5 minutes
                return Math.min(delay, maxDelay);

            default:
                return 0;
        }
    }

    /**
     * Get human-readable description of retry strategy
     */
    public String getRetryDescription(Task task) {
        switch (task.getRetryStrategy()) {
            case NONE:
                return "No retry";
            case IMMEDIATE:
                return String.format("Immediate retry (max %d attempts)", task.getMaxRetryCount());
            case FIXED_DELAY:
                return String.format("Fixed delay of %d seconds (max %d attempts)", 
                    task.getRetryDelay(), task.getMaxRetryCount());
            case EXPONENTIAL_BACKOFF:
                return String.format("Exponential backoff starting at %d seconds (max %d attempts)", 
                    task.getRetryBackoff(), task.getMaxRetryCount());
            default:
                return "Unknown";
        }
    }
}
```

---

## 5. CREATE RetrySchedulerService.java
**Location:** `src/main/java/com/project/taskscheduler/service/RetrySchedulerService.java`

```java
package com.project.taskscheduler.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.model.TaskExecutionHistory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RetrySchedulerService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private RetryPolicyService retryPolicyService;

    /**
     * Schedule a retry for a failed task
     */
    public void scheduleRetry(Task task, TaskExecutionHistory failedExecution, Exception error) {
        try {
            // Check if retry should happen
            if (!retryPolicyService.shouldRetry(task, failedExecution)) {
                log.info("Task {} will not be retried", task.getTaskId());
                return;
            }

            // Calculate retry attempt number
            int currentAttempt = failedExecution.getRetryAttempt() != null ? failedExecution.getRetryAttempt() : 0;
            int nextAttempt = currentAttempt + 1;

            // Calculate delay
            long delaySeconds = retryPolicyService.calculateRetryDelay(task, nextAttempt);

            // Calculate start time
            LocalDateTime startTime = LocalDateTime.now().plusSeconds(delaySeconds);
            Date startDate = Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());

            // Load job class
            Class<? extends Job> jobClass = loadJobClass(task.getJobClass());

            // Create unique job key for retry
            String retryJobName = String.format("retry-task-%d-attempt-%d-%d", 
                task.getTaskId(), nextAttempt, System.currentTimeMillis());
            JobKey jobKey = JobKey.jobKey(retryJobName, "retries");

            // Prepare job data
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("taskId", task.getTaskId());
            dataMap.put("retryAttempt", nextAttempt);
            dataMap.put("parentExecutionId", failedExecution.getExecutionId());
            dataMap.put("isRetry", true);

            // Create job detail
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobKey)
                    .usingJobData(dataMap)
                    .build();

            // Create trigger
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + retryJobName, "retries")
                    .startAt(startDate)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                    .build();

            // Schedule the retry
            scheduler.scheduleJob(jobDetail, trigger);

            log.info("Scheduled retry for task {} (attempt {}/{}) in {} seconds at {}", 
                task.getTaskId(), nextAttempt, task.getMaxRetryCount(), delaySeconds, startTime);

        } catch (Exception e) {
            log.error("Failed to schedule retry for task {}: {}", task.getTaskId(), e.getMessage(), e);
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
```

---

## 6. UPDATE TaskExecutionHistoryService.java - Add retry methods
**Location:** `src/main/java/com/project/taskscheduler/service/TaskExecutionHistoryService.java`

Add these methods to your existing service:

```java
/**
 * Record execution start for a retry attempt
 */
public Long recordRetryExecutionStart(Long taskId, String taskName, int retryAttempt, Long parentExecutionId) {
    String hostname = getHostname();

    TaskExecutionHistory execution = TaskExecutionHistory.builder()
            .taskId(taskId)
            .taskName(taskName)
            .status(ExecutionStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .executedBy(hostname)
            .retryAttempt(retryAttempt)
            .parentExecutionId(parentExecutionId)
            .isRetry(true)
            .build();

    TaskExecutionHistory saved = taskExecutionHistoryRepository.save(execution);
    return saved.getExecutionId();
}

/**
 * Get execution by ID
 */
public TaskExecutionHistory getExecutionById(Long executionId) {
    return taskExecutionHistoryRepository.findById(executionId).orElse(null);
}

/**
 * Get all retry attempts for a parent execution
 */
public List<TaskExecutionHistory> getRetryHistory(Long parentExecutionId) {
    return taskExecutionHistoryRepository.findByParentExecutionId(parentExecutionId);
}
```

---

## 7. UPDATE EmailJob.java - Integrate retry logic
**Location:** `src/main/java/com/project/taskscheduler/jobs/EmailJob.java`

Replace entire file with:

```java
package com.project.taskscheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.repository.TaskRepository;
import com.project.taskscheduler.service.EmailService;
import com.project.taskscheduler.service.RetrySchedulerService;
import com.project.taskscheduler.service.TaskExecutionHistoryService;

public class EmailJob implements Job {

    @Autowired
    private EmailService emailService;

    @Autowired
    private TaskExecutionHistoryService executionHistoryService;

    @Autowired
    private RetrySchedulerService retrySchedulerService;

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
            String to = "riyajindal2808@gmail.com";
            String subject = "Task Execution Notification";
            String body = "Hello Riya! Your scheduled task (ID: " + taskId + ") just executed successfully at: " 
                              + java.time.LocalDateTime.now() + " Task Scheduler System";

            emailService.sendEmail(to, subject, body);
            System.out.println("Email sent successfully for taskId : " + taskId);
            
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
                    retrySchedulerService.scheduleRetry(task, failedExecution, e);
                }
            }
            System.err.println("Failed to send email: " + e.getMessage());
            throw new JobExecutionException(e);
        }
    }
}
```

---

## 8. UPDATE LogTimeJob.java - Integrate retry logic
**Location:** `src/main/java/com/project/taskscheduler/jobs/LogTimeJob.java`

Replace entire file with:

```java
package com.project.taskscheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.repository.TaskRepository;
import com.project.taskscheduler.service.RetrySchedulerService;
import com.project.taskscheduler.service.TaskExecutionHistoryService;

public class LogTimeJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(LogTimeJob.class);

    @Autowired
    private TaskExecutionHistoryService executionHistoryService;

    @Autowired
    private RetrySchedulerService retrySchedulerService;

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
                    retrySchedulerService.scheduleRetry(task, failedExecution, e);
                }
            }
            log.error("[LogTimeJob] Execution failed for taskId={}", taskId, e);
            throw new JobExecutionException(e);
        }
    }
}
```

---

## 9. OPTIONAL: CREATE TaskRetryController.java (For API endpoints)
**Location:** `src/main/java/com/project/taskscheduler/controller/TaskRetryController.java`

```java
package com.project.taskscheduler.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.taskscheduler.model.TaskExecutionHistory;
import com.project.taskscheduler.service.TaskExecutionHistoryService;

@RestController
@RequestMapping("/api/retry")
public class TaskRetryController {

    @Autowired
    private TaskExecutionHistoryService executionHistoryService;

    /**
     * Get retry history for a specific execution
     * Example: GET /api/retry/execution/123/history
     */
    @GetMapping("/execution/{executionId}/history")
    public ResponseEntity<List<TaskExecutionHistory>> getRetryHistory(@PathVariable Long executionId) {
        List<TaskExecutionHistory> retries = executionHistoryService.getRetryHistory(executionId);
        return ResponseEntity.ok(retries);
    }
}
```

---

## TESTING THE RETRY MECHANISM

### 1. Create a task with retry configuration:

```json
POST http://localhost:8000/api/tasks/create
{
  "name": "Test Retry Task",
  "cronExpression": "0 0/5 * * * ?",
  "jobClass": "com.project.taskscheduler.jobs.EmailJob",
  "active": true,
  "retryStrategy": "EXPONENTIAL_BACKOFF",
  "maxRetryCount": 3,
  "retryDelay": 30,
  "retryBackoff": 1
}
```

### 2. Monitor execution history:

```
GET http://localhost:8000/api/execution-history/task/{taskId}
```

### 3. Check retry history for a failed execution:

```
GET http://localhost:8000/api/retry/execution/{executionId}/history
```

---

## HOW IT WORKS

1. **Failure Handling**: Task executes and fails. Exception is caught in the Job.
2. **Recording**: `executionHistoryService.recordExecutionFailure()` logs the failure.
3. **Triggering Retry**: `retrySchedulerService.scheduleRetry()` is called.
4. **Policy Check**: `RetryPolicyService` checks:
    - Is retry strategy != NONE?
    - Are we under max retry attempts?
5. **Backoff**: If yes, calculates delay (e.g., 2s, 4s, 8s).
6. **Scheduling**: A new, one-off Job is scheduled with the calculated delay.
7. **Execution**: Retry job executes, logging itself as a retry (`isRetry=true`).
8. **Loop**: If it fails again, the cycle repeats until success or max retries.

---

## RETRY STRATEGY EXAMPLES

### IMMEDIATE (no delay):
- Task fails at **10:00:00**
- Retry 1 at **10:00:00** (0s delay)
- Retry 2 at **10:00:00** (0s delay)

### FIXED_DELAY (30 seconds):
- Task fails at **10:00:00**
- Retry 1 at **10:00:30**
- Retry 2 at **10:01:00**

### EXPONENTIAL_BACKOFF (starting at 1 second):
- Task fails at **10:00:00**
- Retry 1 at **10:00:01** (1s)
- Retry 2 at **10:00:03** (2s)
- Retry 3 at **10:00:07** (4s)
- Retry 4 at **10:00:15** (8s)
