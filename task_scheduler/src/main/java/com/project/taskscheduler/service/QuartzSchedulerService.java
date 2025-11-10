package com.project.taskscheduler.service;

import java.util.List;
import java.util.Optional;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.Trigger;
import org.springframework.stereotype.Service;

import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.repository.TaskRepository;

@Service
public class QuartzSchedulerService {
    private final Scheduler scheduler;
    private final TaskRepository tasksRepo;

    public QuartzSchedulerService(Scheduler scheduler, TaskRepository taskRepository) {
        this.scheduler = scheduler;
        this.tasksRepo = taskRepository;
    }

    public void scheduleTask(Task task) throws Exception {
        if (task == null || Boolean.FALSE.equals(task.getActive())) {
            unscheduleTask(task);
            return;
        }
        
    // Dynamically load job class from the 'jobClass' field in DB
        Class<? extends Job> jobClass = loadJobClass(task.getJobClass());

        JobKey jobKey = JobKey.jobKey("task-" + task.getTaskId(), "tasks");
        TriggerKey triggerKey = TriggerKey.triggerKey("trigger-" + task.getTaskId(), "tasks");

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("taskId", task.getTaskId());

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .usingJobData(dataMap)
                .build();

        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule(task.getCronExpression())
                .withMisfireHandlingInstructionDoNothing();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(cron)
                .forJob(jobDetail)
                .build();

        // If the job already exists, update it
        if (scheduler.checkExists(jobKey)) {
            scheduler.addJob(jobDetail, true);              // update job detail
            scheduler.rescheduleJob(triggerKey, trigger);   // update trigger
        } else {
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }  
    public void unscheduleTask(Task task) throws SchedulerException {
        if (task == null || task.getTaskId() == null) return;

        JobKey jobKey = JobKey.jobKey("task-" + task.getTaskId(), "tasks");
        TriggerKey triggerKey = TriggerKey.triggerKey("trigger-" + task.getTaskId(), "tasks");

        if (scheduler.checkExists(triggerKey)) {
            scheduler.unscheduleJob(triggerKey);
        }
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    public void scheduleAllActiveFromDb() throws Exception {
        List<Task> activeTasks = tasksRepo.findByActiveTrue();
        for (Task task : activeTasks) {
            scheduleTask(task);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends Job> loadJobClass(String className) throws ClassNotFoundException {
        Class<?> className1 = Class.forName(className);
        if (!Job.class.isAssignableFrom(className1)) {
            throw new IllegalArgumentException("Class " + className + " does not implement org.quartz.Job");
        }
        return (Class<? extends Job>) className1;
    }
}
