package com.project.task_scheduler.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.config.Task;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.task_scheduler.model.Tasks;
import com.project.task_scheduler.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping("/create")
    public Tasks createOrUpdateTasks(@RequestBody Tasks task){
        return taskService.saveTask(task);
    }

    @GetMapping("/all")
    public List<Tasks> getAllTasks(){
        return taskService.getAll();
    }

    @GetMapping("/{taskId}")
    public Optional<Tasks> getTaskById(@PathVariable Long taskId){
        return taskService.getTaskById(taskId);
    }

    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return "Task deleted successfully (ID: " + taskId + ")";
    }

    @GetMapping("/active")
    public List<Tasks> getActiveTasks() {
        return taskService.getActiveTasks();
    }

    @PutMapping("/{id}/status")
    public Tasks toggleTaskStatus(@PathVariable Long id, @RequestParam boolean active) {
        return taskService.toggleTaskStatus(id, active);
    }


}
