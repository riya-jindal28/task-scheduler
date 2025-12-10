package com.project.taskscheduler.controller;

import org.springframework.web.bind.annotation.RestController;

import com.project.taskscheduler.model.TaskExecutionHistory;
import com.project.taskscheduler.service.TaskExecutionHistoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/execution-history")
public class TaskExecutionHistoryController {
    
    @Autowired
    private TaskExecutionHistoryService taskExecutionHistoryService;

    @GetMapping("/task/{taskId}")
    public ResponseEntity<Page<TaskExecutionHistory>> getTaskExecutionHistory(@PathVariable Long taskId,
        @RequestParam(defaultValue = "0") int page ,
        @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TaskExecutionHistory> history = taskExecutionHistoryService.getTaskExecutionHistory(taskId, pageable);
        return ResponseEntity.ok(history);
    }

    
}
