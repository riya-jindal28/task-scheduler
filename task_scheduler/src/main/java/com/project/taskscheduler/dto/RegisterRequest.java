package com.project.taskscheduler.dto;

import com.project.taskscheduler.model.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String username;
    private String password;
    private Role role = Role.ROLE_USER; // Default role
}