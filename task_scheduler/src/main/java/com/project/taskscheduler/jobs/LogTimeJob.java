package com.project.taskscheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogTimeJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(LogTimeJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = (Long) context.getMergedJobDataMap().get("taskId"); // optional info passed from scheduler
        log.info("[LogTimeJob] Executed at={} (taskId={})", java.time.LocalDateTime.now(), taskId);
    }
} 
