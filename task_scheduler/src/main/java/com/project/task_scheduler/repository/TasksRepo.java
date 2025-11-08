package com.project.task_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.task_scheduler.model.Tasks;

import java.util.List;

@Repository
public interface TasksRepo extends JpaRepository<Tasks, Long>{
    
    List<Tasks> findByActiveTrue();
}
