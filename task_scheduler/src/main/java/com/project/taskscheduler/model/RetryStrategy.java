package com.project.taskscheduler.model;

public enum RetryStrategy {
    NONE,
    IMMEDIATE,
    FIXED_DELAY,           //Wait the same delay (e.g 30 seconds) before each retry attempt
    EXPONENTIAL_DELAY     //Wait and increase the delay (e.g 30s, 1m, etc.) before each retry attempt
}
