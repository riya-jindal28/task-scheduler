package com.project.taskscheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.model.TaskExecutionHistory;
import com.project.taskscheduler.repository.TaskRepository;
import com.project.taskscheduler.service.EmailService;
import com.project.taskscheduler.service.RetryService;
import com.project.taskscheduler.service.TaskExecutionHistoryService;

public class EmailJob implements Job {

    @Autowired
    private EmailService emailService;

    @Autowired
    private TaskExecutionHistoryService executionHistoryService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RetryService retryService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = (Long) context.getMergedJobDataMap().get("taskId");
        String taskName = context.getJobDetail().getKey().getName();

        Boolean isRetry = (Boolean) context.getMergedJobDataMap().get("isRetry");
        Integer retryAttempt = (Integer) context.getMergedJobDataMap().get("retryAttempt");
        Long parentExecutionId = (Long) context.getMergedJobDataMap().get("parentExecutionId");
        Long executionId = null;

        try {
            if (isRetry != null && isRetry) {
                executionId = executionHistoryService.recordRetryExecutionStart(
                        taskId, taskName, retryAttempt, parentExecutionId);
            } else {
                executionId = executionHistoryService.recordExecutionStart(taskId, taskName);
            }

            String to = "riyajindal2808@gmail.com";
            String subject = "Task Execution Notification";
            String body = "Hello Riya! Your scheduled task (ID: " + taskId + ") just executed successfully at: "
                    + java.time.LocalDateTime.now() + "Task Scheduler System";

            emailService.sendEmail(to, subject, body);
            System.out.println("Email sent successfully for taskId : " + taskId);
            executionHistoryService.recordExecutionSuccess(executionId);

        } catch (Exception e) {
            if (executionId != null) {
                TaskExecutionHistory failedExecution = executionHistoryService.recordExecutionFailure(executionId, e);

                // Schedule retry if configured
                Task task = taskRepository.findById(taskId).orElse(null);
                if (task != null) {
                    System.out.println("DEBUG: EmailJob failed. Checking retry for task " + taskId);
                    System.out.println("DEBUG: Strategy: " + task.getRetryStrategy());

                    if (failedExecution != null) {
                        retryService.scheduleRetry(task, failedExecution, e);
                    }
                } else {
                    System.out.println("DEBUG: Task not found for ID " + taskId);
                }
            }
            System.err.println("Failed to send email: " + e.getMessage());
            throw new JobExecutionException(e);
        }
    }
}
