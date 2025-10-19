package com.kaiburr.task2.service;

import com.kaiburr.task2.model.Task;
import com.kaiburr.task2.model.TaskExecution;
import com.kaiburr.task2.repository.TaskRepository;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TaskService {

    private final TaskRepository repo;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final KubernetesClient k8sClient;

    public TaskService(TaskRepository repo, KubernetesClient k8sClient) {
        this.repo = repo;
        this.k8sClient = k8sClient;
    }

    public List<Task> findAll() { return repo.findAll(); }
    public Task findById(String id) { return repo.findById(id).orElse(null); }
    public Task save(Task t) { return repo.save(t); }
    public void deleteById(String id) { repo.deleteById(id); }
    public List<Task> findByNameContains(String s) { return repo.findByNameContainingIgnoreCase(s); }

    private void validateCommand(String cmd) {
        String lower = cmd.toLowerCase();
        String[] forbidden = {"rm ","sudo","shutdown","reboot",">", "|","&&",";","`","curl ","wget ","nc ","mkfs","dd"};
        for(String f : forbidden) {
            if(lower.contains(f)) throw new IllegalArgumentException("Forbidden token in command: "+f);
        }
    }

    public TaskExecution runCommandAndRecord(Task task) {
        validateCommand(task.getCommand());
        Instant start = Instant.now();

        try {
            Pod pod = new PodBuilder()
                    .withNewMetadata().withGenerateName("task-pod-").endMetadata()
                    .withNewSpec()
                        .addNewContainer()
                        .withName("task-container")
                        .withImage("busybox")
                        .withCommand("sh", "-c", task.getCommand())
                        .endContainer()
                        .withRestartPolicy("Never")
                    .endSpec()
                    .build();

            pod = k8sClient.pods().inNamespace("default").create(pod);

            int tries = 0;
            while(tries < 30) {
                Pod current = k8sClient.pods().inNamespace("default").withName(pod.getMetadata().getName()).get();
                String phase = current.getStatus().getPhase();
                if("Succeeded".equals(phase) || "Failed".equals(phase)) break;
                Thread.sleep(1000);
                tries++;
            }

            String output = k8sClient.pods().inNamespace("default")
                    .withName(pod.getMetadata().getName())
                    .inContainer("task-container")
                    .getLog();

            Instant end = Instant.now();
            TaskExecution exec = new TaskExecution();
            exec.setStartTime(start);
            exec.setEndTime(end);
            exec.setOutput(output);

            task.getTaskExecutions().add(exec);
            repo.save(task);

            k8sClient.pods().inNamespace("default").withName(pod.getMetadata().getName()).delete();

            return exec;

        } catch (KubernetesClientException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
