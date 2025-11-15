package com.project.taskscheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import com.project.taskscheduler.service.EmailService;


public class EmailJob implements Job {

    @Autowired
    private EmailService emailService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
       try{
        Long taskId = (Long) context.getMergedJobDataMap().get("taskId");
        String to = "riyajindal2808@gmail.com";
        String subject = "Task Execution Notification";
        String body = "Hello Riya! 👋\n\nYour scheduled task (ID: " + taskId + ") just executed successfully at " 
                          + java.time.LocalDateTime.now() + "\n\n— Task Scheduler System";

        emailService.sendEmail(to, subject, body);
        System.out.println("✅ Email sent successfully for taskId=" + taskId);

       }
       catch (Exception e){
        System.err.println("Failed to send email: " + e.getMessage());
            throw new JobExecutionException(e);
       }
    }
}

