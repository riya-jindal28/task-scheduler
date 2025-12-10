package com.project.taskscheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import com.project.taskscheduler.service.EmailService;
import com.project.taskscheduler.service.TaskExecutionHistoryService;


public class EmailJob implements Job {

    @Autowired
    private EmailService emailService;

    @Autowired
    private TaskExecutionHistoryService executionHistoryService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = (Long) context.getMergedJobDataMap().get("taskId");
        String taskName = context.getJobDetail().getKey().getName();
        Long executionId = null; 

       try{
        executionId = executionHistoryService.recordExecutionStart(taskId, taskName);

        String to = "riyajindal2808@gmail.com";
        String subject = "Task Execution Notification";
        String body = "Hello Riya! Your scheduled task (ID: " + taskId + ") just executed successfully at: " 
                          + java.time.LocalDateTime.now() + "Task Scheduler System";

        emailService.sendEmail(to, subject, body);
        System.out.println("Email sent successfully for taskId : " + taskId);
        executionHistoryService.recordExecutionSuccess(executionId);

       }
       catch (Exception e){
        if (executionId != null){
            executionHistoryService.recordExecutionFailure(executionId, e);
        }
        System.err.println("Failed to send email: " + e.getMessage());
            throw new JobExecutionException(e);
       }
    }
}

