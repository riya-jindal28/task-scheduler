package com.project.task_scheduler.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.task_scheduler.model.Tasks;
import com.project.task_scheduler.repository.TasksRepo;

@Service
public class TaskService {
    
    @Autowired
    private TasksRepo taskRepository;


    public Tasks saveTask(Tasks task) {
        return taskRepository.save(task);
    }

    public Optional<Tasks> getTaskById(Long taskId) {
        return taskRepository.findById(taskId);
    }

    public List<Tasks> getAll(){
        return taskRepository.findAll();
    }

    public List<Tasks> getActiveTasks() {
        return taskRepository.findByActiveTrue();
    }

    public void deleteTask(Long taskId){
        taskRepository.deleteById(taskId);
    }

    public Tasks toggleTaskStatus(Long taskId, boolean active) {
        Optional<Tasks> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            Tasks task = taskOpt.get();
            task.setActive(active);
            return taskRepository.save(task);
        }
        return null;
    }


}
