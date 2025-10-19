package com.kaiburr.task2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Arrays;

@RestController
public class TaskController {

    @GetMapping("/tasks")
    public List<String> getTasks() {
        // Example data
        return Arrays.asList("Task 1", "Task 2", "Task 3");
    }
}
