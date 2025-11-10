package com.project.taskscheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.taskscheduler.model.Task;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>{
    
    List<Task> findByActiveTrue();
}
