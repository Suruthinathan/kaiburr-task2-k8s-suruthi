package com.kaiburr.task2.controller;

import com.kaiburr.task2.model.Task;
import com.kaiburr.task2.model.TaskExecution;
import com.kaiburr.task2.repository.TaskRepository;
import com.kaiburr.task2.util.K8sCommandRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final K8sCommandRunner k8sRunner;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        this.k8sRunner = new K8sCommandRunner();
    }

    // ===========================================================
    // GET /tasks - all tasks or by ID
    // ===========================================================
    @GetMapping
    public List<Task> getTasks(@RequestParam(required = false) String id) {
        if (id != null) {
            return taskRepository.findById(id)
                    .map(List::of)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        }
        return taskRepository.findAll();
    }

    // ===========================================================
    // GET /tasks/search?name=<name> - search by name
    // ===========================================================
    @GetMapping("/search")
    public List<Task> searchTasks(@RequestParam String name) {
        List<Task> tasks = taskRepository.findByNameContainingIgnoreCase(name);
        if (tasks.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No tasks found");
        return tasks;
    }

    // ===========================================================
    // POST /tasks - create new task
    // ===========================================================
    @PostMapping
    public Task createTask(@RequestBody Task task) {
        if (!isValidCommand(task.getCommand())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsafe command detected");
        }
        return taskRepository.save(task);
    }

    // ===========================================================
    // PUT /tasks - create or update a task
    // ===========================================================
    @PutMapping
    public Task createOrUpdateTask(@RequestBody Task task) {
        if (!isValidCommand(task.getCommand())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsafe command detected");
        }
        return taskRepository.save(task);
    }

    // ===========================================================
    // DELETE /tasks/{id} - delete a task by ID
    // ===========================================================
    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable String id) {
        if (!taskRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        taskRepository.deleteById(id);
    }

    // ===========================================================
    // PUT /tasks/{id}/execution - run command in Kubernetes pod
    // ===========================================================
    @PutMapping("/{id}/execution")
    public TaskExecution executeTask(@PathVariable String id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (!isValidCommand(task.getCommand())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsafe command detected");
        }

        try {
            TaskExecution execution = new TaskExecution();
            execution.setStartTime(new Date());

            // Convert to command array for busybox: ["sh", "-c", "<command>"]
            String[] commandArray = {"sh", "-c", task.getCommand()};
            String output = k8sRunner.runCommandInPod("default", "task-" + task.getId(), commandArray, 60);

            execution.setEndTime(new Date());
            execution.setOutput(output);

            task.getTaskExecutions().add(execution);
            taskRepository.save(task);

            return execution;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Execution failed: " + e.getMessage());
        }
    }

    // ===========================================================
    // Command safety validation
    // ===========================================================
    private boolean isValidCommand(String command) {
        if (command == null) return false;
        String lower = command.toLowerCase();
        return !(lower.contains("rm") ||
                lower.contains("dd") ||
                lower.contains(":(){") ||
                lower.contains("> /dev") ||
                lower.contains("shutdown") ||
                lower.contains("reboot") ||
                lower.contains("wget") ||
                lower.contains("curl") ||
                lower.contains("mkfs"));
    }
}
