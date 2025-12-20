package com.project.taskscheduler.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.project.taskscheduler.service.EmailService;

@Component
public class GlobalJobListener implements JobListener {

    @Autowired
    private EmailService emailService;

    @Override
    public String getName() {
        return "GlobalJobFailureListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        // No action needed before execution
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // No action needed if vetoed
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        // If an exception ocurred, it means the job FAILED
        if (jobException != null) {
            String jobName = context.getJobDetail().getKey().getName();
            String errorMsg = jobException.getMessage();
            
            System.out.println("GlobalListener caught failure for: " + jobName);
            
            // Send Alert Email
            emailService.sendAlert(
                "Job Execution Failed: " + jobName, 
                "CRITICAL ALERT : The job '" + jobName + "' has failed.\n\nError Details: " + errorMsg
            );
        }
    }
}
