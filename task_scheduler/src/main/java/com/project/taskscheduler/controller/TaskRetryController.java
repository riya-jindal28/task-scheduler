package com.project.taskscheduler.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.taskscheduler.model.TaskExecutionHistory;
import com.project.taskscheduler.service.TaskExecutionHistoryService;

@RestController
@RequestMapping("/api/retry")
public class TaskRetryController {

    @Autowired
    private TaskExecutionHistoryService executionHistoryService;

    @GetMapping("/execution/{executionId}/history")
    public ResponseEntity<List<TaskExecutionHistory>> getRetryHistory(@PathVariable Long executionId) {
        List<TaskExecutionHistory> retries = executionHistoryService.getRetryHistory(executionId);
        return ResponseEntity.ok(retries);
    }


    @GetMapping("/task/{taskId}/history")
    public ResponseEntity<List<TaskExecutionHistory>> getTaskRetryHistory(@PathVariable Long taskId) {
        List<TaskExecutionHistory> retries = executionHistoryService.getRetryHistoryForTask(taskId);
        return ResponseEntity.ok(retries);
    }

}
