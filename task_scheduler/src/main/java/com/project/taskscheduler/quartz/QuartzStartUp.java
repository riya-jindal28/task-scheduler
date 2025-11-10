package com.project.taskscheduler.quartz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.project.taskscheduler.service.QuartzSchedulerService;

@Component
public class QuartzStartUp implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuartzStartUp.class);

    private final QuartzSchedulerService schedulerService;

    public QuartzStartUp(QuartzSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("🚀 Initializing Quartz Scheduler...");
        schedulerService.scheduleAllActiveFromDb();
        log.info("✅ All active tasks have been scheduled successfully!");
    }
}

    