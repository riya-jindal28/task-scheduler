package com.project.taskscheduler.service;

import java.util.List;
import java.util.Optional;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.repository.TaskRepository;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private QuartzSchedulerService quartzSchedulerService;


    public Task saveTask(Task task) throws Exception {
        quartzSchedulerService.scheduleTask(task);
        return taskRepository.save(task);

    }

    public Optional<Task> getTaskById(Long taskId) {
        return taskRepository.findById(taskId);
    }

    public List<Task> getAll(){
        return taskRepository.findAll();
    }

    public List<Task> getActiveTasks() {
        return taskRepository.findByActiveTrue();
    }

    public void deleteTask(Long taskId) throws SchedulerException{
        Optional<Task> optionalTask = taskRepository.findById(taskId);
        if (optionalTask.isPresent()) {
            Task task = optionalTask.get();

            // Unschedule it first before deleting
            quartzSchedulerService.unscheduleTask(task);
        taskRepository.deleteById(taskId);
    }
}

    public Task toggleTaskStatus(Long taskId, boolean active) throws Exception {
    Optional<Task> taskOpt = taskRepository.findById(taskId);
    if (taskOpt.isPresent()) {
        Task task = taskOpt.get();

        task.setActive(active);
        Task updated = taskRepository.save(task);

        if(active) {
            quartzSchedulerService.scheduleTask(updated);
        }
        else{
            quartzSchedulerService.unscheduleTask(updated);
        }
        return updated;
    }
    return null;
}


}
