package com.project.taskscheduler.controller;

import java.util.List;
import java.util.Optional;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.taskscheduler.model.Task;
import com.project.taskscheduler.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping("/create")
    public Task createOrUpdateTasks(@RequestBody Task task) throws Exception{
        return taskService.saveTask(task);
    }

    @GetMapping("/all")
    public List<Task> getAllTasks(){
        return taskService.getAll();
    }

    @GetMapping("/{taskId}")
    public Optional<Task> getTaskById(@PathVariable Long taskId){
        return taskService.getTaskById(taskId);
    }

    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable Long taskId) throws SchedulerException {
        taskService.deleteTask(taskId);
        return "Task deleted successfully (ID: " + taskId + ")";
    }

    @GetMapping("/active")
    public List<Task> getActiveTasks() {
        return taskService.getActiveTasks();
    }

    @PutMapping("/{id}/status")
    public Task toggleTaskStatus(@PathVariable Long id, @RequestParam boolean active) throws Exception {
        return taskService.toggleTaskStatus(id, active);
    }


}
