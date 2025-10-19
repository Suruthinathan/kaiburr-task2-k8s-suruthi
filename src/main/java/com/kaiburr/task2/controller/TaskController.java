package com.kaiburr.task2.controller;

import com.kaiburr.task2.model.Task;
import com.kaiburr.task2.model.TaskExecution;
import com.kaiburr.task2.service.K8sPodService;
import com.kaiburr.task2.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final K8sPodService k8sPodService;

    public TaskController(TaskService taskService, K8sPodService k8sPodService) {
        this.taskService = taskService;
        this.k8sPodService = k8sPodService;
    }

    // Get all tasks
    @GetMapping
    public List<Task> getAll() {
        return taskService.findAll();
    }

    // Get task by ID
    @GetMapping("/{id}")
    public ResponseEntity<Task> getById(@PathVariable String id) {
        Task t = taskService.findById(id);
        return t != null ? ResponseEntity.ok(t) : ResponseEntity.notFound().build();
    }

    // Create or update task
    @PutMapping
    public Task createOrUpdate(@RequestBody Task task) {
        return taskService.save(task);
    }

    // Delete task
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        taskService.deleteById(id);
    }

    // Find tasks by name
    @GetMapping("/find")
    public ResponseEntity<List<Task>> findByName(@RequestParam String name) {
        List<Task> list = taskService.findByNameContains(name);
        return list.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(list);
    }

    // Execute task — create a Kubernetes pod
    @PutMapping("/{id}/execution")
    public ResponseEntity<String> execute(@PathVariable String id) {
        Task t = taskService.findById(id);
        if (t == null) return ResponseEntity.notFound().build();

        try {
            // Create a Kubernetes pod with task-id in name
            String podName = "task-" + id;
            String podImage = "busybox"; // you can use any image you want
            k8sPodService.createPod(podName, podImage);

            return ResponseEntity.ok("Kubernetes Pod for task " + id + " created: " + podName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to create Kubernetes pod: " + e.getMessage());
        }
    }
}
