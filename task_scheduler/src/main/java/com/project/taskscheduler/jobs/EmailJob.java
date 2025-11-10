package com.project.taskscheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class EmailJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(EmailJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = (Long) context.getMergedJobDataMap().get("taskId");
        log.info("[EmailJob] Executed successfully for taskId={}", taskId);
    }
}

